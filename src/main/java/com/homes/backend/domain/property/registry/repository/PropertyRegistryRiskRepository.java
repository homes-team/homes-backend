package com.homes.backend.domain.property.registry.repository;

import com.homes.backend.domain.property.registry.entity.PropertyRegistryRisk;
import com.homes.backend.domain.property.registry.entity.RegistryRiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PropertyRegistryRiskRepository extends JpaRepository<PropertyRegistryRisk, Long> {
    Optional<PropertyRegistryRisk> findByPropertyId(Long propertyId);

    /**
     * 재스캔 결과 반영. mock 스캐너는 매물 ID만 보고 결정적인 값을 내놓기 때문에 이전 스캔과 값이 완전히 같을 수 있는데,
     * 엔티티 dirty checking에 맡기면 값이 같을 때 UPDATE 자체가 안 나가서 updated_at("최종 스캔일시")이 안 갱신된다.
     * 그래서 값이 같든 다르든 항상 UPDATE가 나가도록 벌크 쿼리로 직접 처리하고, updated_at도 여기서 같이 갱신한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PropertyRegistryRisk r SET r.mortgageCount = :mortgageCount, r.seizureCount = :seizureCount, " +
            "r.riskLevel = :riskLevel, r.summary = :summary, r.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE r.property.id = :propertyId")
    void updateScanResult(
            @Param("propertyId") Long propertyId,
            @Param("mortgageCount") Integer mortgageCount,
            @Param("seizureCount") Integer seizureCount,
            @Param("riskLevel") RegistryRiskLevel riskLevel,
            @Param("summary") String summary
    );
}
