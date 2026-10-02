package com.homes.backend.domain.property.insight.service;

import java.util.List;
import java.util.Optional;

public interface AiEvaluationReportGenerator {
    boolean isConfigured();

    String modelVersion();

    Optional<GeneratedReport> generate(String evaluationInputJson);

    record GeneratedReport(String summary, List<String> strengths, List<String> weaknesses) {
    }
}
