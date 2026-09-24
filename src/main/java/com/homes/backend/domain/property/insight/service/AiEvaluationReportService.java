package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.CategoryScore;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.Report;
import com.homes.backend.domain.property.insight.entity.PropertyAiReport;
import com.homes.backend.domain.property.insight.repository.PropertyAiReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class AiEvaluationReportService {
    static final String RULE_BASED_MODEL_VERSION = "RULE_BASED_REPORT_V1";

    private final PropertyAiReportRepository reportRepository;
    private final AiEvaluationReportGenerator reportGenerator;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentMap<Long, ReentrantLock> propertyLocks = new ConcurrentHashMap<>();

    public AiEvaluationReportService(
            PropertyAiReportRepository reportRepository,
            AiEvaluationReportGenerator reportGenerator,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.reportRepository = reportRepository;
        this.reportGenerator = reportGenerator;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Resolution resolve(
            Property property,
            List<CategoryScore> categories,
            String scoreVersion,
            String notice,
            Report fallback,
            LocalDateTime fallbackGeneratedAt
    ) {
        long availableCount = categories.stream().filter(category -> category.rawScore() != null).count();
        if (availableCount < 2 || !reportGenerator.isConfigured()) {
            return fallback(fallback, fallbackGeneratedAt);
        }

        String inputJson;
        String inputHash;
        try {
            inputJson = objectMapper.writeValueAsString(input(property, categories, scoreVersion, notice));
            inputHash = sha256(reportGenerator.modelVersion() + "|" + inputJson);
        } catch (Exception exception) {
            log.warn("AI 매물 리포트 입력 직렬화에 실패하여 규칙 기반 리포트를 사용합니다", exception);
            return fallback(fallback, fallbackGeneratedAt);
        }

        ReentrantLock lock = propertyLocks.computeIfAbsent(property.getId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            Resolution resolution = transactionTemplate.execute(status ->
                    resolveInTransaction(property.getId(), inputHash, inputJson, notice));
            return resolution == null ? fallback(fallback, fallbackGeneratedAt) : resolution;
        } catch (Exception exception) {
            log.warn("AI 매물 리포트 저장에 실패하여 규칙 기반 리포트를 사용합니다", exception);
            return fallback(fallback, fallbackGeneratedAt);
        } finally {
            lock.unlock();
            propertyLocks.remove(property.getId(), lock);
        }
    }

    private Resolution resolveInTransaction(Long propertyId, String inputHash, String inputJson, String notice) {
        return reportRepository.findByPropertyIdAndInputHash(propertyId, inputHash)
                .map(this::toResolution)
                .orElseGet(() -> reportGenerator.generate(inputJson)
                        .map(generated -> save(propertyId, inputHash, notice, generated))
                        .orElse(null));
    }

    private Resolution save(
            Long propertyId,
            String inputHash,
            String notice,
            AiEvaluationReportGenerator.GeneratedReport generated
    ) {
        try {
            String strengthsJson = objectMapper.writeValueAsString(generated.strengths());
            String weaknessesJson = objectMapper.writeValueAsString(generated.weaknesses());
            LocalDateTime generatedAt = LocalDateTime.now();
            PropertyAiReport report = reportRepository.findById(propertyId)
                    .orElseGet(() -> new PropertyAiReport(
                            propertyId,
                            inputHash,
                            reportGenerator.modelVersion(),
                            generated.summary(),
                            strengthsJson,
                            weaknessesJson,
                            notice,
                            generatedAt
                    ));
            report.update(
                    inputHash,
                    reportGenerator.modelVersion(),
                    generated.summary(),
                    strengthsJson,
                    weaknessesJson,
                    notice,
                    generatedAt
            );
            return toResolution(reportRepository.save(report));
        } catch (Exception exception) {
            throw new IllegalStateException("AI report persistence failed", exception);
        }
    }

    private Resolution toResolution(PropertyAiReport report) {
        try {
            List<String> strengths = objectMapper.readValue(
                    report.getStrengthsJson(), new TypeReference<List<String>>() {});
            List<String> weaknesses = objectMapper.readValue(
                    report.getWeaknessesJson(), new TypeReference<List<String>>() {});
            return new Resolution(
                    new Report(report.getSummary(), strengths, weaknesses, report.getNotice()),
                    report.getModelVersion(),
                    report.getGeneratedAt()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Stored AI report is invalid", exception);
        }
    }

    private Map<String, Object> input(
            Property property,
            List<CategoryScore> categories,
            String scoreVersion,
            String notice
    ) {
        Map<String, Object> propertyInput = new LinkedHashMap<>();
        propertyInput.put("propertyId", property.getId());
        propertyInput.put("title", property.getTitle());
        propertyInput.put("propertyType", property.getPropertyType() == null
                ? null : property.getPropertyType().name());
        propertyInput.put("area", property.getArea());
        propertyInput.put("currentFloor", property.getCurrentFloor());
        propertyInput.put("totalFloors", property.getTotalFloors());
        propertyInput.put("direction", property.getDirection() == null
                ? null : property.getDirection().name());
        propertyInput.put("remodelingYear", property.getRemodelingYear());

        List<Map<String, Object>> categoryInputs = categories.stream().map(category -> {
            Map<String, Object> categoryInput = new LinkedHashMap<>();
            categoryInput.put("key", category.key().name());
            categoryInput.put("label", category.label());
            categoryInput.put("rawScore", category.rawScore());
            categoryInput.put("displayScore", category.displayScore());
            categoryInput.put("status", category.status().name());
            categoryInput.put("source", category.source().name());
            categoryInput.put("description", category.description());
            categoryInput.put("evidence", category.evidence());
            categoryInput.put("calculation", category.calculation());
            return categoryInput;
        }).toList();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("scoreVersion", scoreVersion);
        input.put("property", propertyInput);
        input.put("categories", categoryInputs);
        input.put("notice", notice);
        return input;
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }

    private Resolution fallback(Report report, LocalDateTime generatedAt) {
        return new Resolution(report, RULE_BASED_MODEL_VERSION, generatedAt);
    }

    public record Resolution(Report report, String modelVersion, LocalDateTime generatedAt) {
    }
}
