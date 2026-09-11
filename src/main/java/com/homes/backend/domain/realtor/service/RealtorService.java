package com.homes.backend.domain.realtor.service;

import com.homes.backend.domain.bid.entity.BidStatus;
import com.homes.backend.domain.bid.repository.AgentFeeByPropertyTypeProjection;
import com.homes.backend.domain.bid.repository.BidRepository;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.repository.PropertyDistanceProjection;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.realtor.dto.request.AgentUpdateProfileReqDto;
import com.homes.backend.domain.realtor.dto.request.RealtorSignupReqDto;
import com.homes.backend.domain.realtor.dto.response.AgentDashboardStatsResDto;
import com.homes.backend.domain.realtor.dto.response.AgentProfileResDto;
import com.homes.backend.domain.realtor.dto.response.NearbyPropertyResDto;
import com.homes.backend.domain.realtor.dto.response.RealtorPublicProfileResDto;
import com.homes.backend.domain.realtor.dto.response.RealtorSignupResDto;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.exception.RealtorErrorCode;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.review.repository.ReviewRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.geocoding.GeocodedPoint;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.storage.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtorService {

    private static final int NEARBY_PROPERTIES_LIMIT = 20;

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final PropertyRepository propertyRepository;
    private final BidRepository bidRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RealtorAccountWriter realtorAccountWriter;
    private final GeocodingService geocodingService;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RealtorSignupResDto signUp(RealtorSignupReqDto request) {
        // 1. 이메일 DB 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 사전에 완료된 이메일 인증인지 확인 (일반 회원가입과 동일한 인증 절차)
        String isVerified = (String) redisTemplate.opsForValue().get("AUTH_SUCCESS:" + request.email());
        if (isVerified == null || !isVerified.equals("TRUE")) {
            throw new CustomException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 3. 사업자등록번호 중복 검사
        if (agentRepository.existsByBusinessNum(request.businessNum())) {
            throw new CustomException(RealtorErrorCode.DUPLICATE_BUSINESS_NUM);
        }

        // 3-1. 업로드된 서류/프로필 이미지 검증 - 용량 확인 + 이 이메일로 발급받은 URL이 맞는지 대조 후 소비 처리
        String uploaderIdentity = "email:" + request.email();
        s3PresignedUrlService.validateAndConsumeUploadedFile(request.businessCertUrl(), uploaderIdentity);
        s3PresignedUrlService.validateAndConsumeUploadedFile(request.agentCertUrl(), uploaderIdentity);
        if (request.profileImageUrl() != null) {
            s3PresignedUrlService.validateAndConsumeUploadedFile(request.profileImageUrl(), uploaderIdentity);
        }

        // 4. 유저 계정 생성
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .role("AGENT")
                .build();

        // 5. 사무소 주소 -> 위경도 자동 변환 (실패해도 가입 자체는 막지 않음)
        Optional<GeocodedPoint> geocodedPoint = geocodingService.geocode(request.officeAddress());
        Double officeLatitude = geocodedPoint.map(GeocodedPoint::latitude).orElse(null);
        Double officeLongitude = geocodedPoint.map(GeocodedPoint::longitude).orElse(null);

        // 6. 중개사 프로필 생성 (관리자 승인 전까지 isVerified=false로 대기). 서류 이미지는 클라이언트가
        // presigned URL로 이미 S3에 올려서 URL만 넘어오므로, 여기서 별도 업로드/롤백 처리는 필요 없다.
        Agent savedAgent = realtorAccountWriter.write(user, request, officeLatitude, officeLongitude);

        // 가입에 사용된 이메일 인증 증표는 파기
        redisTemplate.delete("AUTH_SUCCESS:" + request.email());

        return RealtorSignupResDto.from(savedAgent.getUser(), savedAgent);
    }

    /**
     * 중개사 마이페이지 - 본인 프로필 수정 (사무소 정보만).
     * PATCH 시맨틱: 값을 보내지 않은(null) 필드는 기존 값을 그대로 유지한다.
     */
    @Transactional
    public AgentProfileResDto updateMyProfile(Long userId, AgentUpdateProfileReqDto request) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        String officeName = request.officeName() != null ? request.officeName() : agent.getOfficeName();
        String officeAddress = request.officeAddress() != null ? request.officeAddress() : agent.getOfficeAddress();

        Double officeLatitude = agent.getOfficeLatitude();
        Double officeLongitude = agent.getOfficeLongitude();

        // 주소가 바뀌면 위경도도 새로 계산한다 (실패 시 null로 - 옛 주소의 좌표를 그대로 남겨두면 주소-좌표가 서로 어긋나게 됨)
        if (request.officeAddress() != null) {
            Optional<GeocodedPoint> geocodedPoint = geocodingService.geocode(request.officeAddress());
            officeLatitude = geocodedPoint.map(GeocodedPoint::latitude).orElse(null);
            officeLongitude = geocodedPoint.map(GeocodedPoint::longitude).orElse(null);
        }

        agent.updateOfficeProfile(officeName, officeAddress, officeLatitude, officeLongitude);

        return AgentProfileResDto.from(agent);
    }

    /**
     * 중개사 마이페이지 - 본인 프로필 조회
     */
    public AgentProfileResDto getMyProfile(Long userId) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        return AgentProfileResDto.from(agent);
    }

    /**
     * 중개사 마이페이지 - 사무소 기준 거리순으로 정렬된 거래가능 매물 목록 조회
     */
    public List<NearbyPropertyResDto> getNearbyAvailableProperties(Long userId) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        if (agent.getOfficeLatitude() == null || agent.getOfficeLongitude() == null) {
            throw new CustomException(RealtorErrorCode.OFFICE_LOCATION_NOT_SET);
        }

        List<PropertyDistanceProjection> nearbyProperties = propertyRepository.findByStatusOrderByDistance(
                PropertyStatus.AVAILABLE,
                agent.getOfficeLatitude(),
                agent.getOfficeLongitude(),
                NEARBY_PROPERTIES_LIMIT
        );

        return nearbyProperties.stream()
                .map(NearbyPropertyResDto::from)
                .toList();
    }

    /**
     * 중개사 마이페이지 - 입찰가능 매물 목록: 거래가능 매물 중 아직 내가 입찰을 넣지 않은 것만 거리순으로 조회
     */
    public List<NearbyPropertyResDto> getBiddableProperties(Long userId) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        if (agent.getOfficeLatitude() == null || agent.getOfficeLongitude() == null) {
            throw new CustomException(RealtorErrorCode.OFFICE_LOCATION_NOT_SET);
        }

        List<PropertyDistanceProjection> biddableProperties = propertyRepository.findBiddableByAgentOrderByDistance(
                PropertyStatus.AVAILABLE,
                agent.getOfficeLatitude(),
                agent.getOfficeLongitude(),
                agent.getId(),
                NEARBY_PROPERTIES_LIMIT
        );

        return biddableProperties.stream()
                .map(NearbyPropertyResDto::from)
                .toList();
    }

    /**
     * 중개사 마이페이지 통계 - 이번 달 거래 건수 + 매물 종류별 평균 확정 수수료(전체 기간)
     */
    public AgentDashboardStatsResDto getMyDashboardStats(Long userId) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        long thisMonthCompletedDealsCount = bidRepository.countCompletedDealsInRange(
                agent.getId(), BidStatus.ACCEPTED, PropertyStatus.COMPLETED, monthStart, nextMonthStart
        );

        List<AgentFeeByPropertyTypeProjection> feeProjections = bidRepository.findAverageFeeByPropertyType(agent.getId());
        List<AgentDashboardStatsResDto.AverageFeeByPropertyType> averageFees = feeProjections.stream()
                .map(p -> new AgentDashboardStatsResDto.AverageFeeByPropertyType(p.getPropertyType(), p.getAverageFee()))
                .toList();

        return new AgentDashboardStatsResDto(thisMonthCompletedDealsCount, averageFees);
    }

    /**
     * 유저가 보는 중개사 상세 프로필 - 성사율(입찰 수락 비율) + 리뷰 평점/건수 포함
     */
    public RealtorPublicProfileResDto getPublicProfile(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        long totalBidCount = bidRepository.countByAgentId(agentId);
        Double successRate = null;
        if (totalBidCount > 0) {
            long acceptedBidCount = bidRepository.countByAgentIdAndStatus(agentId, BidStatus.ACCEPTED);
            successRate = acceptedBidCount * 100.0 / totalBidCount;
        }

        Long targetUserId = agent.getUser().getId();
        Double averageReviewScore = reviewRepository.findAverageScoreByTargetUserId(targetUserId);
        long reviewCount = reviewRepository.countByTargetUserId(targetUserId);

        return RealtorPublicProfileResDto.of(agent, successRate, averageReviewScore, reviewCount);
    }

    /**
     * agentId(중개사 프로필 ID)로 리뷰 대상이 되는 User ID를 조회 (리뷰 조회/작성 시 사용)
     */
    public Long resolveUserIdByAgentId(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        return agent.getUser().getId();
    }
}
