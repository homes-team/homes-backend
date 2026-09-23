package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyDirection;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.entity.PropertyAiReport;
import com.homes.backend.domain.property.insight.repository.PropertyAiReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiEvaluationReportServiceTest {
    private PropertyAiReportRepository repository;
    private AiEvaluationReportGenerator generator;
    private AiEvaluationReportService service;
    private Property property;
    private List<AiEvaluationRespDto.CategoryScore> categories;
    private AiEvaluationRespDto.Report fallback;
    private LocalDateTime fallbackGeneratedAt;

    @BeforeEach
    void setUp() {
        repository = mock(PropertyAiReportRepository.class);
        generator = mock(AiEvaluationReportGenerator.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new AiEvaluationReportService(repository, generator, new ObjectMapper(), transactionManager);

        property = Property.builder()
                .title("테스트 매물")
                .area(23.0)
                .currentFloor(3)
                .totalFloors(5)
                .direction(PropertyDirection.SOUTH)
                .remodelingYear(2022)
                .build();
        setPropertyId(property, 21L);
        categories = List.of(
                category(AiEvaluationRespDto.CategoryKey.SCHOOL, 80.0),
                category(AiEvaluationRespDto.CategoryKey.TRANSPORT, 70.0),
                category(AiEvaluationRespDto.CategoryKey.NATURE, 60.0),
                category(AiEvaluationRespDto.CategoryKey.SUNLIGHT, 90.0)
        );
        fallback = new AiEvaluationRespDto.Report("규칙 기반", List.of(), List.of(), null);
        fallbackGeneratedAt = LocalDateTime.of(2026, 9, 23, 12, 0);
    }

    @Test
    void usesRuleBasedReportWhenOpenAiIsNotConfigured() {
        when(generator.isConfigured()).thenReturn(false);

        AiEvaluationReportService.Resolution result = service.resolve(
                property, categories, "SCORE_V1", null, fallback, fallbackGeneratedAt);

        assertThat(result.report()).isEqualTo(fallback);
        assertThat(result.modelVersion()).isEqualTo(AiEvaluationReportService.RULE_BASED_MODEL_VERSION);
        verifyNoInteractions(repository);
    }

    @Test
    void generatesAndStoresOpenAiReportWhenCacheIsMissing() {
        when(generator.isConfigured()).thenReturn(true);
        when(generator.modelVersion()).thenReturn("OPENAI_TEST_V1");
        when(repository.findByPropertyIdAndInputHash(eq(21L), anyString())).thenReturn(Optional.empty());
        when(repository.findById(21L)).thenReturn(Optional.empty());
        when(generator.generate(anyString())).thenReturn(Optional.of(
                new AiEvaluationReportGenerator.GeneratedReport(
                        "생활 여건이 우수합니다.",
                        List.of("일조량이 좋습니다."),
                        List.of("자연 점수는 보통입니다.")
                )));
        when(repository.save(any(PropertyAiReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiEvaluationReportService.Resolution result = service.resolve(
                property, categories, "SCORE_V1", null, fallback, fallbackGeneratedAt);

        assertThat(result.modelVersion()).isEqualTo("OPENAI_TEST_V1");
        assertThat(result.report().summary()).isEqualTo("생활 여건이 우수합니다.");
        ArgumentCaptor<PropertyAiReport> captor = ArgumentCaptor.forClass(PropertyAiReport.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getInputHash()).hasSize(64);
    }

    private AiEvaluationRespDto.CategoryScore category(
            AiEvaluationRespDto.CategoryKey key,
            double rawScore
    ) {
        return new AiEvaluationRespDto.CategoryScore(
                key,
                key.name(),
                rawScore,
                rawScore / 20.0,
                AiEvaluationRespDto.ScoreStatus.AVAILABLE,
                AiEvaluationRespDto.ScoreSource.EXTERNAL_DATA,
                "설명"
        );
    }

    private void setPropertyId(Property target, Long id) {
        try {
            var field = Property.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
