package com.homes.backend.domain.review.repository;

import com.homes.backend.domain.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = "reviewer")
    List<Review> findAllByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    boolean existsByReviewerIdAndTargetUserId(Long reviewerId, Long targetUserId);

    long countByTargetUserId(Long targetUserId);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.targetUser.id = :targetUserId")
    Double findAverageScoreByTargetUserId(@Param("targetUserId") Long targetUserId);
}
