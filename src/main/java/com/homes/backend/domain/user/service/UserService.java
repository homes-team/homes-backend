package com.homes.backend.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.user.dto.request.AdminIdentityVerificationSyncReqDto;
import com.homes.backend.domain.user.dto.request.EmailVerificationReqDto;
import com.homes.backend.domain.user.dto.request.IdentityVerificationReqDto;
import com.homes.backend.domain.user.dto.request.UserCreateReqDto;
import com.homes.backend.domain.user.dto.request.UserLoginReqDto;
import com.homes.backend.domain.user.dto.request.UserUpdatePasswordReqDto;
import com.homes.backend.domain.user.dto.request.UserUpdateProfileReqDto;
import com.homes.backend.domain.user.dto.response.IdentityVerificationResDto;
import com.homes.backend.domain.user.dto.response.UserDetailResDto;
import com.homes.backend.domain.user.dto.response.UserProfileResDto;
import com.homes.backend.domain.user.dto.response.UserSignupResDto;
import com.homes.backend.domain.user.dto.response.UserUpdateProfileResDto;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.property.repository.PropertyFavoriteRepository;
import com.homes.backend.domain.property.repository.RecentViewRepository;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.review.repository.ReviewRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.security.JwtTokenProvider;
import com.homes.backend.global.security.TokenDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final PropertyFavoriteRepository propertyFavoriteRepository;
    private final RecentViewRepository recentViewRepository;
    private final AgentRepository agentRepository;
    private final ReviewRepository reviewRepository;
    private static final long AUTH_CODE_EXPIRATION = 300L; // 인증번호 유효시간: 5분 (300초)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    public void checkEmailDuplication(String email) {
        if (userRepository.existsByEmail(email)) {
            // 중복 시 에러 코드 실어 핸들러가 가로챔
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 마이페이지 회원정보 조회
     * @param userId 토큰에서 추출한 로그인한 유저의 고유 ID
     */
    public UserProfileResDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return UserProfileResDto.from(user);
    }

    /**
     * 공인중개사가 매물 등록자(유저)의 평판/정보를 확인하기 위한 상세 조회.
     * 비밀번호/이메일/전화번호 등 개인정보는 노출하지 않는다.
     */
    public UserDetailResDto getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Double averageReviewScore = reviewRepository.findAverageScoreByTargetUserId(userId);
        long reviewCount = reviewRepository.countByTargetUserId(userId);

        return UserDetailResDto.of(user, averageReviewScore, reviewCount);
    }

    @Transactional
    public UserSignupResDto signUp(UserCreateReqDto request) {
        // 1. 이메일 DB 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        String isVerified = (String) redisTemplate.opsForValue().get("AUTH_SUCCESS:" + request.email());

        if (isVerified == null || !isVerified.equals("TRUE")) {
            throw new CustomException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 2. 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);

        // 가입이 완벽히 끝났으니 Redis에 있던 합격증은 깔끔하게 파기
        redisTemplate.delete("AUTH_SUCCESS:" + request.email());

        // 3. 응답 DTO로 변환하여 반환
        return UserSignupResDto.from(savedUser);
    }

    public TokenDto login(UserLoginReqDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(UserErrorCode.WRONG_PASSWORD);
        }

        //1. 30분, 14일짜리 토큰 세트를 구움
        TokenDto tokenDto = jwtTokenProvider.createTokenSet(user.getId(), user.getEmail(), user.getRole());

        //2. Refresh Token을 Redis에 "RT:유저ID"를 Key로 해서 박음 (2주 수명 똑같이 세팅)
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                tokenDto.refreshToken(),
                tokenDto.refreshTokenExpirationTime(),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        return tokenDto;
    }

    /**
     * 비밀번호 수정 기능
     * @param userId 토큰에서 추출한 로그인한 유저의 고유 ID
     */
    @Transactional
    public void updatePassword(Long userId, UserUpdatePasswordReqDto request) {
        // 1. 토큰 주인(유저)이 진짜 DB에 존재하는지 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 입력한 '현재 비밀번호'가 DB에 저장된 암호화된 비밀번호와 일치하는지 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(UserErrorCode.WRONG_PASSWORD); // 현재 비밀번호 틀림 에러
        }

        // 3. 검증이 끝났다면, '새로운 비밀번호'를 다시 BCrypt로 암호화
        String encodedPassword = passwordEncoder.encode(request.newPassword());

        // 4. 엔티티의 비밀번호를 업데이트
        user.updatePassword(encodedPassword);
    }

    /**
     * 마이페이지 회원정보(닉네임, 이용 목적) 수정.
     * 실명(name)/전화번호(phone)는 실명 인증(verifyIdentity)으로만 변경 가능하므로 여기서는 다루지 않는다.
     * PATCH 시맨틱: 값을 보내지 않은(null) 필드는 기존 값을 그대로 유지한다.
     * @param userId 토큰에서 추출한 로그인한 유저의 고유 ID
     */
    @Transactional
    public UserUpdateProfileResDto updateProfile(Long userId, UserUpdateProfileReqDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (request.nickname() != null && userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        String nickname = request.nickname() != null ? request.nickname() : user.getNickname();
        String usagePurpose = request.usagePurpose() != null ? request.usagePurpose() : user.getUsagePurpose();
        user.updateProfile(nickname, usagePurpose);

        try {
            // saveAndFlush로 즉시 UPDATE를 날려, DB unique 제약 위반을 여기서 바로 잡는다
            // (Dirty Checking에만 맡기면 트랜잭션 커밋 시점까지 반영이 미뤄져 여기서 못 잡음)
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        return UserUpdateProfileResDto.from(user);
    }

    /**
     * 회원 탈퇴: row를 물리적으로 지우지 않고 개인정보만 지우는 소프트 삭제(익명화)로 처리한다.
     * 매물/리뷰/채팅/알림 등 여러 테이블이 user_id를 참조하고 있어 물리 삭제하면 그만큼 FK 위반 위험이 있고,
     * 신고·거래 기록 같은 감사 데이터도 탈퇴 후에 남아있어야 하므로, 유저 row 자체는 남기고 anonymize()만 호출한다.
     * 본인 소유의 찜/최근 본 방 기록/중개사 프로필은 더 이상 의미가 없는 개인화 데이터라 함께 정리한다.
     * @param userId 토큰에서 추출한 로그인한 유저의 고유 ID
     */
    @Transactional
    public void deleteAccount(Long userId, String accessToken) {
        anonymizeAndCleanup(userId, null);

        // 지금 요청에 쓰인 Access Token도 만료 전까지 즉시 무효화 (본인 탈퇴에만 해당 - 이 요청 자체가 그 토큰으로 인증됨)
        blacklistAccessToken(accessToken);
    }

    /**
     * 관리자용 강제 탈퇴. 본인 탈퇴(deleteAccount)와 같은 익명화 로직을 쓰되,
     * 관리자가 대상 유저의 accessToken을 알 수 없으므로 그 토큰을 블랙리스트에 올리는 절차는 없다
     * (이메일이 스크럽되므로 기존 토큰은 다음 인증 시점에 자연히 무효화된다).
     */
    @Transactional
    public void forceWithdraw(Long userId, String reason) {
        anonymizeAndCleanup(userId, reason);
    }

    private void anonymizeAndCleanup(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        propertyFavoriteRepository.deleteAllByUserId(userId);
        recentViewRepository.deleteAllByUserId(userId);
        agentRepository.deleteByUserId(userId);
        user.anonymize(reason);

        // 탈퇴한 계정으로 새 토큰을 재발급받지 못하도록 Refresh Token도 함께 폐기
        redisTemplate.delete("RT:" + userId);
    }

    //Redis를 활용한 로그아웃 (블랙리스트 등록)
    @Transactional
    public void logout(String accessToken) {
        blacklistAccessToken(accessToken);
    }

    // 남은 유효시간만큼 Redis에 "BLACK:" 접두사로 등록해 해당 Access Token을 즉시 무효화
    private void blacklistAccessToken(String accessToken) {
        // 1. 토큰이 무조건 "Bearer "로 시작하니까 알맹이만 쏙 빼내기
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 2. 이 토큰이 앞으로 얼마나 더 살아있을 수 있는지 남은 유효시간(ms) 계산
        Long expiration = jwtTokenProvider.getExpiration(accessToken);

        // 3. 만약 이미 만료된 토큰이 아니라면, 남은 시간만큼만 Redis에 "BLACK:" 접두사를 붙여 저장!
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    "BLACK:" + accessToken,
                    "logout",
                    expiration,
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
        }
    }

    //1. 이메일 인증번호 요청 (+ 중복 검사)
    @Transactional
    public void sendVerificationCode(String email) {
        // DB 중복 검사
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 6자리 랜덤 인증번호 생성
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // Redis에 5분간 저장
        redisTemplate.opsForValue().set(
                "AUTH:" + email,
                verificationCode,
                java.util.concurrent.TimeUnit.SECONDS.toMillis(AUTH_CODE_EXPIRATION),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            // 생성자 단계에서 명확하게 "UTF-8" 인코딩을 강제로 선언해버립니다.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 로그인 계정 주소와 메일 보내는 명의를 일치시키는 설정
            helper.setFrom("homescompanyduksung@gmail.com");

            // [조치] 제목에서 구글이 오해할만한 대괄호([])를 빼고 직관적으로 적기
            helper.setSubject("Homes Verification Code");

            helper.setTo(email); // 받는 사람 이메일 (스웨거에 치는 주소로 배달)

            // 메일 본문 내용
            String content = "<div style='margin:20px;'>" +
                    "<h3>Homes 회원가입 인증번호 안내</h3>" +
                    "<p>아래 인증번호를 화면에 입력해주세요.</p>" +
                    "<h2 style='color:#304FFE;'>" + verificationCode + "</h2>" +
                    "<p>본 인증번호는 5분간 유효합니다.</p>" +
                    "</div>";

            helper.setText(content, true); // HTML 형식 지정

            // 발송
            mailSender.send(message);
            //System.out.println("[성공] 진짜 사용자의 이메일함으로 메일 전송 완료!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    //2. 이메일 인증번호 검증
    public void verifyCode(EmailVerificationReqDto request) {
        //Redis 금고에서 이메일에 해당하는 인증번호 꺼내오기
        String savedCode = (String) redisTemplate.opsForValue().get("AUTH:" + request.email());

        //인증번호가 만료되었거나 없는 경우
        if (savedCode == null) {
            throw new CustomException(UserErrorCode.VERIFICATION_CODE_EXPIRED); // 에러코드 정의 필요!
        }

        //사용자가 입력한 번호와 금고의 번호가 일치하는지 대조
        if (!savedCode.equals(request.code())) {
            throw new CustomException(UserErrorCode.WRONG_VERIFICATION_CODE); // 에러코드 정의 필요!
        }

        //검증 완료 시, 회원가입 때 인증된 사람이 맞는지 최종 확인할 수 있도록
        //Redis에 "AUTH_SUCCESS:" 상태를 5분간 남겨두면 가입 API 보안을 더 올릴 수 있음
        redisTemplate.opsForValue().set("AUTH_SUCCESS:" + request.email(), "TRUE", 5, java.util.concurrent.TimeUnit.MINUTES);

        // 검증 성공했으니 기존 인증 데이터는 Redis에서 삭제 처리
        redisTemplate.delete("AUTH:" + request.email());
    }

    //Refresh Token을 대조하여 토큰 세트를 통째로 재발급
    @Transactional
    public TokenDto refresh(String refreshToken) {
        // 1. Bearer 떼어내기 공정
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        // 2. 만료되었거나 망가진 Refresh Token이면 에러
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 토큰에서 주인(UserId) 꺼내오기
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 4. 주인 ID로 유저 정보 리얼 DB에서 다시 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 5. Redis에 보관 중인 진짜 Refresh Token 꺼내기
        String savedRefreshToken = (String) redisTemplate.opsForValue().get("RT:" + userId);

        // 6. 비었거나 사용자가 들고 온 거랑 다르면 "해킹 위험" 차단
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 7. 검증 통과. 토큰 둘 다 새로 구움
        TokenDto newTokenDto = jwtTokenProvider.createTokenSet(user.getId(), user.getEmail(), user.getRole());

        // 8. Redis에 새 Refresh Token으로 갈아끼우기
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                newTokenDto.refreshToken(),
                newTokenDto.refreshTokenExpirationTime(),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        return newTokenDto;
    }

    @Transactional
    public TokenDto googleLogin(String authorizationCode) {
        // 1. 사용자의 구글 이메일 긁어오기 (구글 서버와 실시간 HTTP 통신)
        String email = getGoogleEmail(authorizationCode);

        // 2. 회원가입 및 로그인 처리 분기
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // DB에 없으면 신규 구글 소셜 가입 진행
                    User newUser = User.builder()
                            .email(email)
                            // 소셜 가입자는 비밀번호가 필요 없으므로 UUID 난수로 안전하게 암호화 처리
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. 토큰 세트(Access/Refresh) 구워내기
        TokenDto tokenDto = jwtTokenProvider.createTokenSet(user.getId(), user.getEmail(), user.getRole());

        // 4. Redis 금고에 리프레시 토큰 안전하게 세팅
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                tokenDto.refreshToken(),
                tokenDto.refreshTokenExpirationTime(),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        return tokenDto;
    }

    private String getGoogleEmail(String authorizationCode) throws CustomException {
        // 타임아웃을 3초(3000ms)로 제한하는 RestTemplate 세팅
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000); // 연결 제한 시간 3초
        requestFactory.setReadTimeout(3000);    // 데이터 응답 제한 시간 3초

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";

            // 1. 헤더 설정 (Form URL Encoded 방식 지정)
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
            tokenBody.add("code", authorizationCode);
            tokenBody.add("client_id", googleClientId);
            tokenBody.add("client_secret", googleClientSecret);
            tokenBody.add("redirect_uri", googleRedirectUri);
            tokenBody.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, String.class);

            // 구글이 준 JSON 데이터에서 google_access_token 빼내기
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            String googleAccessToken = tokenJson.get("access_token").asText();

            // 긁어온 구글 Access Token으로 유저 프로필(이메일) 요청
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(googleAccessToken); // Header에 'Bearer 구글토큰' 장착

            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
            ResponseEntity<String> userResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userRequest, String.class);

            // 구글이 준 유저 정보 JSON 데이터 파싱
            JsonNode userJson = objectMapper.readTree(userResponse.getBody());

            //구글이 인증한 진짜 이메일인지 체크
            boolean isVerified = userJson.has("verified_email") && userJson.get("verified_email").asBoolean();

            if (!isVerified) {
                // 인증되지 않은 이메일이면 예외를 던져서 진입을 컷
                throw new CustomException(UserErrorCode.INVALID_GOOGLE_CODE);
            }

            // 검증이 완료된 안전한 메일 주소만 리턴
            return userJson.get("email").asText();

        } catch (Exception e) {
            // 구글 통신 장애나 변조된 코드일 경우 예외 처리
            throw new CustomException(UserErrorCode.INVALID_GOOGLE_CODE);
        }
    }

    // 실명 인증: 본인(유저)이 프론트에서 포트원 인증을 마치고 identityVerificationId를 넘겨줌
    @Transactional
    public IdentityVerificationResDto verifyIdentity(Long userId, IdentityVerificationReqDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (user.isIdentityVerified()) {
            throw new CustomException(UserErrorCode.ALREADY_IDENTITY_VERIFIED);
        }

        JsonNode detail = getPortOneIdentityVerification(request.identityVerificationId());
        applyVerifiedIdentity(user, detail);

        return IdentityVerificationResDto.from(user);
    }

    // 실명 인증 강제 동기화: 포트원은 실명 인증 완료를 알려주는 웹훅을 제공하지 않으므로,
    // "인증은 했는데 마이페이지에 반영이 안 됐다" 문의가 왔을 때 관리자가 재조회해서 수동으로 맞춰주는 예외 처리용 API
    @Transactional
    public IdentityVerificationResDto forceSyncIdentityVerification(AdminIdentityVerificationSyncReqDto request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        JsonNode detail = getPortOneIdentityVerification(request.identityVerificationId());
        applyVerifiedIdentity(user, detail);

        return IdentityVerificationResDto.from(user);
    }

    private void applyVerifiedIdentity(User user, JsonNode detail) {
        String status = detail.path("status").asText();
        if (!"VERIFIED".equals(status)) {
            throw new CustomException(UserErrorCode.IDENTITY_VERIFICATION_NOT_COMPLETED);
        }

        JsonNode verifiedCustomer = detail.path("verifiedCustomer");
        String name = verifiedCustomer.path("name").asText(null);
        String phone = verifiedCustomer.path("phoneNumber").asText(null);
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            throw new CustomException(UserErrorCode.IDENTITY_VERIFICATION_NOT_COMPLETED);
        }
        user.verifyIdentity(name, phone);
    }

    // 포트원 V2 본인인증 단건 조회 API 호출 (GET /identity-verifications/{id})
    private JsonNode getPortOneIdentityVerification(String identityVerificationId) {
        // 연결 3초, 응답 60초로 제한 (포트원 권장 타임아웃)
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(60000);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneApiSecret);

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        String encodedId = URLEncoder.encode(identityVerificationId, StandardCharsets.UTF_8);
        String url = "https://api.portone.io/identity-verifications/" + encodedId;

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            return new ObjectMapper().readTree(response.getBody());
        } catch (Exception e) {
            throw new CustomException(UserErrorCode.PORTONE_COMMUNICATION_ERROR);
        }
    }

}