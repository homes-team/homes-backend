package com.homes.backend.domain.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "infrastructure", indexes = {
        @Index(name = "idx_infra_type", columnList = "infra_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Infrastructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "infra_type", nullable = false)
    private InfraType infraType;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    // PostGIS 공간 인덱스를 활용하기 위한 Geometry 타입
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Builder
    public Infrastructure(String name, InfraType infraType, Double lat, Double lon, Point location) {
        this.name = name;
        this.infraType = infraType;
        this.lat = lat;
        this.lon = lon;
        this.location = location;
    }
}
