package com.homes.backend.domain.property.entity;

import com.homes.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(
        name = "property_favorite",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_property_favorite",
                        columnNames = {"user_id", "property_id"} // 중복 찜 방지
                )
        }
)
public class PropertyFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동으로 id 오름차순 증가
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="property_id",nullable = false)
    private Property property;

    @Builder
    public PropertyFavorite(User user,Property property){
        this.user=user;
        this.property=property;
    }
}
