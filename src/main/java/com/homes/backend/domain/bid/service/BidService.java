package com.homes.backend.domain.bid.service;

import com.homes.backend.domain.bid.dto.request.BidCreateReqDto;
import com.homes.backend.domain.bid.dto.request.NegotiationReqDto;
import com.homes.backend.domain.bid.dto.response.BidListRespDto;
import com.homes.backend.domain.bid.dto.response.NegotiationListResDto;
import com.homes.backend.domain.bid.entity.Bid;
import com.homes.backend.domain.bid.entity.BidStatus;
import com.homes.backend.domain.bid.entity.Negotiation;
import com.homes.backend.domain.bid.exception.BidErrorCode;
import com.homes.backend.domain.bid.repository.BidRepository;
import com.homes.backend.domain.bid.repository.NegotiationRepository;
import com.homes.backend.domain.chat.service.ChatService;
import com.homes.backend.domain.notification.entity.NotificationType;
import com.homes.backend.domain.notification.service.NotificationService;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final PropertyRepository propertyRepository;
    private final AgentRepository agentRepository;
    private final NegotiationRepository negotiationRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    /**
     * 중개사가 수수료 역제안 제시
     */
    @Transactional
    public void createBid(Long propertyId, BidCreateReqDto reqDto, Long userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new CustomException(BidErrorCode.PROPERTY_ALREADY_MATCHED);
        }

        // 입찰하는 중개사(Agent) 조회
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));

        // 이미 해당 매물에 진행 중인(PENDING/ACCEPTED) 입찰서를 제출했는지 검사
        // 취소(CANCELLED)/거절(REJECTED)된 과거 입찰서는 재입찰을 막지 않는다
        if (bidRepository.existsByPropertyIdAndAgentIdAndStatusIn(
                propertyId, agent.getId(), List.of(BidStatus.PENDING, BidStatus.ACCEPTED))) {
            throw new CustomException(BidErrorCode.ALREADY_BIDDED);
        }

        Bid bid = Bid.builder()
                .proposedFee(reqDto.proposedFee())
                .content(reqDto.content())
                .agent(agent)
                .property(property)
                .build();

        bidRepository.save(bid);

        // 새 입찰 제안서가 도착했음을 집주인에게 알림
        notificationService.createNotification(
                property.getUser().getId(), NotificationType.BID, "새로운 입찰 제안서가 도착했습니다.", propertyId);
    }

    /**
     * 특정 매물의 입찰 제안서 리스트 조회 (집주인 전용)
     */
    public List<BidListRespDto> getPropertyBids(Long propertyId, Long userId) {
        Property property = getPropertyAndValidateOwnership(propertyId, userId);

        return bidRepository.findAllByPropertyIdAndStatusInOrderByCreatedAtDesc(
                        propertyId, List.of(BidStatus.PENDING, BidStatus.ACCEPTED))
                .stream()
                .map(BidListRespDto::from)
                .toList();
    }

    /**
     * 역제안 내역 조회
     */
    public List<NegotiationListResDto> getNegotiationList(Long propertyId, Long bidId, Long userId, String role) {
        validateAccessRight(propertyId, bidId, userId, role);

        return negotiationRepository.findAllByBidIdOrderByCreatedAtAsc(bidId).stream()
                .map(NegotiationListResDto::from)
                .toList();
    }

    /**
     * 수수료 역제안 전송
     */
    @Transactional
    public void createNegotiation(Long propertyId, Long bidId, NegotiationReqDto reqDto, Long userId, String role) {
        Bid bid = validateAccessRight(propertyId, bidId, userId, role);

        // 이미 매칭 완료된 제안서면 추가 협상 불가 처리
        if (bid.getStatus() == BidStatus.ACCEPTED) {
            throw new CustomException(BidErrorCode.BID_ALREADY_ACCEPTED);
        }

        // 취소/거절되어 이미 끝난 제안서에는 역제안을 보낼 수 없음 (협상은 PENDING 상태에서만 의미가 있음)
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new CustomException(BidErrorCode.BID_NOT_PENDING);
        }

        Negotiation negotiation = Negotiation.builder()
                .bid(bid)
                .senderRole(role) // "USER" or "AGENT"
                .suggestedFee(reqDto.suggestedFee())
                .message(reqDto.message())
                .build();

        negotiationRepository.save(negotiation);
    }

    /**
     * 중개사 선택 및 매칭 확정
     */
    @Transactional
    public void acceptBid(Long propertyId, Long bidId, Long userId) {
        // 동시성 방어를 위해 '비관적 락'이 걸린 메서드로 매물을 가져옵니다.
        Property property = propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        // 집주인 본인이 맞는지 권한 검증을 직접 해줍니다.
        if (!property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 이 매물이 이미 매칭 완료된 상태라면?
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new CustomException(BidErrorCode.PROPERTY_ALREADY_MATCHED);
        }

        // Bid 상태 확인 및 검증
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new CustomException(BidErrorCode.BID_NOT_FOUND));

        if (!bid.getProperty().getId().equals(propertyId)) {
            throw new CustomException(BidErrorCode.BID_PROPERTY_MISMATCH);
        }

        if (bid.getStatus() == BidStatus.ACCEPTED) {
            throw new CustomException(BidErrorCode.BID_ALREADY_ACCEPTED);
        }

        // 취소/거절되어 이미 끝난 제안서를 다시 살려서 수락하는 것을 방지
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new CustomException(BidErrorCode.BID_NOT_PENDING);
        }

        // 최종 확정 수수료 결정 (가장 마지막 협상 금액, 없으면 최초 입찰 금액)
        List<Negotiation> negotiations = negotiationRepository.findAllByBidIdOrderByCreatedAtAsc(bidId);
        Double finalFee = negotiations.isEmpty() ?
                bid.getProposedFee() : negotiations.get(negotiations.size() - 1).getSuggestedFee();

        // Bid 상태를 ACCEPTED로 변경 및 최종 수수료 저장
        bid.acceptBid(finalFee);

        // 매물 상태도 '매칭 완료'로 변경
        property.matchDeal();

        // 선택받지 못한 다른 입찰서들은 모두 거절 처리
        bidRepository.rejectOtherPendingBids(propertyId, bidId);

        // 매칭 확정된 전속 중개사와의 1:1 채팅방 생성/재오픈
        chatService.createOrReopenRoom(property, property.getUser(), bid.getAgent().getUser());

        // 매칭 확정 알림 (집주인은 본인이 직접 한 행동이라 알 필요 없고, 중개사한테만 알림)
        notificationService.createNotification(
                bid.getAgent().getUser().getId(), NotificationType.MATCHING, "매칭이 확정되었습니다.", propertyId);
    }

    /**
     * 매칭 취소 - 집주인 또는 매칭된 중개사 누구든 요청 가능 (채팅 중 거래가 불발될 수 있으므로)
     */
    @Transactional
    public void cancelBid(Long propertyId, Long bidId, Long userId, String role) {
        // 동시성 방어: complete와 cancel이 동시에 들어와도 한쪽만 처리되도록 매물에 비관적 락을 건다
        Property property = propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        Bid bid = validateAccessRight(propertyId, bidId, userId, role);

        if (bid.getStatus() != BidStatus.ACCEPTED) {
            throw new CustomException(BidErrorCode.BID_NOT_ACCEPTED);
        }

        // Bid.status는 완료된 거래도 ACCEPTED로 유지되므로, 이미 거래완료(COMPLETED)된 매물은 취소 대상에서 제외해야 함
        if (property.getStatus() != PropertyStatus.MATCHED) {
            throw new CustomException(BidErrorCode.PROPERTY_NOT_MATCHED);
        }

        bid.cancelBid();
        property.cancelMatch();
    }

    /**
     * 거래완료 처리 - 매칭 대금을 지불하는 쪽인 집주인만 확정 가능 (중개사가 스스로 완료 처리하면 성과 지표를 조작할 수 있으므로)
     */
    @Transactional
    public void completeBid(Long propertyId, Long bidId, Long userId) {
        // 동시성 방어: complete와 cancel이 동시에 들어와도 한쪽만 처리되도록 매물에 비관적 락을 건다
        Property property = propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new CustomException(BidErrorCode.BID_NOT_FOUND));

        if (!bid.getProperty().getId().equals(propertyId)) {
            throw new CustomException(BidErrorCode.BID_PROPERTY_MISMATCH);
        }

        if (bid.getStatus() != BidStatus.ACCEPTED) {
            throw new CustomException(BidErrorCode.BID_NOT_ACCEPTED);
        }

        if (property.getStatus() != PropertyStatus.MATCHED) {
            throw new CustomException(BidErrorCode.PROPERTY_NOT_MATCHED);
        }

        // Bid.status는 ACCEPTED로 유지한다 - 통계(countCompletedDealsInRange)가
        // "Bid.status=ACCEPTED AND Property.status=COMPLETED" 조합으로 완료 건수를 세기 때문
        property.completeDeal();
    }

    /**
     * [중개사 전용] 대기 중(PENDING)인 입찰 제안서 철회
     */
    @Transactional
    public void withdrawBid(Long propertyId, Long bidId, Long userId) {
        // 동시성 제어: 수락(acceptBid) 로직과 충돌하지 않도록 매물에 비관적 락 획득
        propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        // AGENT 권한 및 본인이 작성한 제안서인지 검증
        Bid bid = validateAccessRight(propertyId, bidId, userId, "AGENT");

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new CustomException(BidErrorCode.BID_NOT_PENDING);
        }

        // 제안서 상태를 철회(WITHDRAWN) 처리
        bid.withdraw();
    }

    /**
     * [집주인 전용] 대기 중(PENDING)인 입찰 제안서 명시적 거절
     */
    @Transactional
    public void rejectBid(Long propertyId, Long bidId, Long userId) {
        // 동시성 제어: 수락(acceptBid) 로직과 충돌하지 않도록 매물에 비관적 락 획득
        propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        // USER 권한 및 본인 소유의 매물인지 검증
        Bid bid = validateAccessRight(propertyId, bidId, userId, "USER");

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new CustomException(BidErrorCode.BID_NOT_PENDING);
        }

        // 제안서 상태를 거절(REJECTED) 처리
        bid.reject();
    }

    // ================= [ 공통 검증 로직 ] =================

    /**
     * 매물 존재 여부 확인 및 집주인(USER) 권한 검증
     */
    private Property getPropertyAndValidateOwnership(Long propertyId, Long userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }
        return property;
    }

    /**
     * 핑퐁(역제안) API 접근 권한 검증
     * - USER(집주인)인 경우: 내 매물이 맞는지 확인
     * - AGENT(중개사)인 경우: 본인이 작성한 입찰서가 맞는지 확인
     */
    private Bid validateAccessRight(Long propertyId, Long bidId, Long userId, String role) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new CustomException(BidErrorCode.BID_NOT_FOUND));

        if (!bid.getProperty().getId().equals(propertyId)) {
            throw new CustomException(BidErrorCode.BID_PROPERTY_MISMATCH);
        }

        /*
         * 역할별 검증
         */
        if ("USER".equals(role)) { // 집주인: 해당 매물의 소유자가 맞는지 검증
            getPropertyAndValidateOwnership(propertyId, userId);
        } else if ("AGENT".equals(role)) { // 중개사: 해당 제안서(bid)를 작성한 중개사가 맞는지 검증
            if (!bid.getAgent().getUser().getId().equals(userId)) {
                throw new CustomException(BidErrorCode.UNAUTHORIZED_BID_ACCESS);
            }
        } else if ("ADMIN".equals(role)) {
            // 관리자(ADMIN): 소유권 검증 없이 그냥 패스
        } else { // USER도 AGENT도 아닌 이상한 값이 들어오면 무조건 튕겨냄
            throw new CustomException(BidErrorCode.UNAUTHORIZED_BID_ACCESS);
        }

        return bid;
    }
}
