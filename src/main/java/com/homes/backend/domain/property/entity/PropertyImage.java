package com.homes.backend.domain.property.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // 로컬/S3에서 받아온 URL

    @Column(nullable = false)
    private Boolean isThumbnail; // 이 사진이 대표 이미지인지 여부 (true/false)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property; // 어떤 매물의 사진인지

    @Builder
    public PropertyImage(String imageUrl, Boolean isThumbnail, Property property) {
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
        this.property = property;
    }
}
