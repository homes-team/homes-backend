package com.homes.backend.domain.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "station")
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String poiId;   // 예: S112
    private String poiName; // 예: 4.19민주묘지역
    private String poiType; // 예: 지하철역

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point coordinate; // 위경도 공간 데이터
}
