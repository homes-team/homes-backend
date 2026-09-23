package com.homes.backend.domain.property.entity;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "properties")
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 유저와 다대일 관계 매핑

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 자동 생성 (예: 강남구 역삼동 신축 원룸)

    @Column(length = 500)
    private String description; // 사용자가 작성

    @Column(nullable = false)
    private String address; // (예: 서울시 강남구 역삼동 123-45)

    @Column(nullable = false)
    private String detailAddress; // (예: 101동 502호)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyType propertyType;

    @Column(nullable = false)
    private Long deposit; // 보증금/전세가/매매가 (단위: 만원)

    @Column(nullable = false)
    private Long monthlyRent; // 월세 (단위: 만원)

    @Column(nullable = false)
    private Long maintenanceFee; // 관리비 (단위: 만원)

    @Column(nullable = false)
    private Integer totalFloors;

    @Column(nullable = false)
    private Integer currentFloor;

    @Enumerated(EnumType.STRING)
    private PropertyDirection direction;

    private Integer remodelingYear;

    /**
     * Returns the stored main-room direction, treating legacy null values as unknown.
     *
     * @return the normalized property direction
     */
    public PropertyDirection getDirection() {
        return direction == null ? PropertyDirection.UNKNOWN : direction;
    }

    @Column(nullable = false)
    private Double area;

    private Integer aiScore;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point coordinate;

    @Column(nullable = false)
    private Double desiredBrokerageFee; // 희망 중개 수수료

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images = new ArrayList<>();

    @BatchSize(size = 100)
    @ElementCollection(targetClass = PropertyOption.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "property_options", joinColumns = @JoinColumn(name = "property_id"))
    private List<PropertyOption> options = new ArrayList<>();

    /**
     * 역세권 정보 (매물 등록 시 1회 계산 후 저장)
     */
    @Column
    private String nearestStation; // 예: 역삼역

    @Column
    private Integer walkingTime; // 도보 시간 (분 단위, 예: 5)

    @Column(nullable = false)
    private Integer favoriteCount = 0;

    @Column(nullable = false)
    private Integer reportCount = 0;

    @Column(nullable = false)
    private boolean isSuspicious = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status; // 거래가능/거래완료

    @Column(name = "deal_completed_at")
    private LocalDateTime dealCompletedAt; // 거래완료 처리된 시점 (거래가능 상태면 NULL)

    /**
     * Creates a property and normalizes an omitted direction to {@link PropertyDirection#UNKNOWN}.
     */
    @Builder
    public Property(User user, String title, String description, String address, String detailAddress,
                    TradeType tradeType, PropertyType propertyType, Long deposit,
                    Long monthlyRent, Long maintenanceFee, Integer totalFloors,
                    Integer currentFloor, PropertyDirection direction, Integer remodelingYear,
                    Double area, Integer aiScore,
                    Point coordinate, Double desiredBrokerageFee,
                    List<PropertyOption> options, String nearestStation, Integer walkingTime, PropertyStatus status) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.address = address;
        this.detailAddress = detailAddress;
        this.tradeType = tradeType;
        this.propertyType = propertyType;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.maintenanceFee = maintenanceFee;
        this.totalFloors = totalFloors;
        this.currentFloor = currentFloor;
        this.direction = direction == null ? PropertyDirection.UNKNOWN : direction;
        this.remodelingYear = remodelingYear;
        this.area = area;
        this.aiScore = aiScore;
        this.coordinate = coordinate;
        this.desiredBrokerageFee = desiredBrokerageFee;
        this.options = options != null ? options : new ArrayList<>();
        this.nearestStation = nearestStation;
        this.walkingTime = walkingTime;
        this.status = status;
    }

    /**
     * 거래 완료 처리
     */
    public void completeDeal() {
        this.status = PropertyStatus.COMPLETED;
        this.dealCompletedAt = LocalDateTime.now();
    }

    /**
     * 중개사 매칭 완료 처리 (집주인이 제안을 수락했을 때)
     */
    public void matchDeal() {
        this.status = PropertyStatus.MATCHED;
    }

    /**
     * 매칭 취소 - 거래가 불발되어 다시 입찰 가능 상태로 되돌림
     */
    public void cancelMatch() {
        this.status = PropertyStatus.AVAILABLE;
    }

    /**
     * 소프트 삭제. 신고/입찰/채팅방 등 다른 데이터가 이 매물을 참조하고 있을 수 있어
     * 물리적으로 지우지 않고 상태만 DELETED로 바꾼다.
     */
    public void markAsDeleted() {
        this.status = PropertyStatus.DELETED;
    }

    /**
     * 의심 매물 상태를 직접 지정한다. 신고 누적 자동 전환과 별개로,
     * 규칙 기반 자동탐지 또는 관리자 수동 지정으로도 호출된다.
     */
    public void markSuspicious(boolean isSuspicious) {
        this.isSuspicious = isSuspicious;
    }

    /**
     * 매물 정보 수정
     */
    public void update(String title, String description, String address, String detailAddress,
                       TradeType tradeType, PropertyType propertyType, Long deposit,
                       Long monthlyRent, Long maintenanceFee, Integer totalFloors,
                       Integer currentFloor, PropertyDirection direction, Integer remodelingYear,
                       Double area, Point coordinate,
                       Double desiredBrokerageFee, List<PropertyOption> options,
                       String nearestStation, Integer walkingTime
    ) {
        this.title = title;
        this.description = description;
        this.address = address;
        this.detailAddress = detailAddress;
        this.tradeType = tradeType;
        this.propertyType = propertyType;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.maintenanceFee = maintenanceFee;
        this.totalFloors = totalFloors;
        this.currentFloor = currentFloor;
        this.direction = direction == null ? PropertyDirection.UNKNOWN : direction;
        this.remodelingYear = remodelingYear;
        this.area = area;
        this.coordinate = coordinate;
        this.desiredBrokerageFee = desiredBrokerageFee;

        this.options.clear();
        if (options != null) {
            this.options.addAll(options);
        }

        this.nearestStation = nearestStation;
        this.walkingTime = walkingTime;
    }
}
