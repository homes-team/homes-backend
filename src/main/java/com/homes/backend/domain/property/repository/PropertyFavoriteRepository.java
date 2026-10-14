package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyFavorite;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.user.entity.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PropertyFavoriteRepository extends JpaRepository<PropertyFavorite, Long> {
    /**
     * 이미 찜한 매물인지 확인
     */
    boolean existsByUserAndProperty(User user, Property property);

    /**
     * 찜 취소 위한 객체 찾기
     */
    Optional<PropertyFavorite> findByUserAndProperty(User user, Property property);

    /**
     * 내가 찜한 매물 리스트 조회
     * 매물 정보 + 찜 매물 한 번에 가져옴. 찜한 뒤 매물이 삭제(DELETED)됐으면 목록에서 제외
     */
    @Query("SELECT pf FROM PropertyFavorite pf JOIN FETCH pf.property WHERE pf.user.id=:userId AND pf.property.status != :excludedStatus")
    List<PropertyFavorite> findAllByUserIdWithProperty(@Param("userId") Long userId, @Param("excludedStatus") PropertyStatus excludedStatus);

    /**
     * 회원 탈퇴 시 본인이 찜한 기록 일괄 삭제
     */
    void deleteAllByUserId(Long userId);
}
