package com.homes.backend.domain.realtor.repository;

import com.homes.backend.domain.realtor.entity.Agent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    boolean existsByBusinessNum(String businessNum);

    /**
     * 회원 탈퇴 시 중개사 프로필 삭제
     */
    void deleteByUserId(Long userId);

    /**
     * 로그인한 유저의 중개사 프로필 조회
     */
    Optional<Agent> findByUserId(Long userId);

    /**
     * 리뷰 등록 시 평판 점수(매너온도) 증감. read-modify-write 대신 DB 원자적 UPDATE로 처리해 동시 리뷰 간 lost update를 방지한다.
     */
    @Modifying
    @Query("UPDATE Agent a SET a.reputationScore = a.reputationScore + :delta WHERE a.id = :agentId")
    void addToReputationScore(@Param("agentId") Long agentId, @Param("delta") float delta);

    /**
     * 관리자용 승인 대기 중개사 목록. 오래 기다린 순으로 정렬
     */
    @EntityGraph(attributePaths = "user")
    List<Agent> findByIsVerifiedFalseOrderByCreatedAtAsc();

    /**
     * 관리자 승인 처리. read-modify-write 대신 원자적 UPDATE로 처리(addToReputationScore와 동일한 이유)
     */
    @Modifying
    @Query("UPDATE Agent a SET a.isVerified = true WHERE a.id = :agentId")
    int approve(@Param("agentId") Long agentId);
}
