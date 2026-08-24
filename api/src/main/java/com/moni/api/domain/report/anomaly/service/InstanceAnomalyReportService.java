package com.moni.api.domain.report.anomaly.service;

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.report.anomaly.dto.AnomalyReportContent;
import com.moni.api.domain.report.anomaly.entity.InstanceAnomalyReport;
import com.moni.api.domain.report.anomaly.repository.InstanceAnomalyReportRepository;
import com.moni.api.domain.report.client.ClaudeReportClient;
import com.moni.api.domain.report.exception.ReportErrorCode;
import com.moni.api.global.error.exception.CustomException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceAnomalyReportService {

    private static final String CLAUDE_MODEL = "claude-haiku-4-5";
    private static final int MAX_TOKENS = 500;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "summary", Map.of("type", "string"),
                    "probable_cause", Map.of("type", "string"),
                    "recommendation", Map.of("type", "string")
            ),
            "required", List.of("summary", "probable_cause", "recommendation"),
            "additionalProperties", false
    );

    private final InstanceRepository instanceRepository;
    private final InstanceThresholdRepository instanceThresholdRepository;
    private final InstanceAnomalyReportRepository instanceAnomalyReportRepository;
    private final ClaudeReportClient claudeReportClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void generateReport(InstanceMetricThresholdExceededEvent event) {
        Instance instance = instanceRepository.findById(event.instanceId())
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));
        InstanceThreshold threshold = instanceThresholdRepository
                .findByInstanceIdAndMetricKey(event.instanceId(), event.metricKey())
                .orElseThrow(() -> new CustomException(InstanceErrorCode.THRESHOLD_NOT_FOUND));

        String userPrompt = AnomalyReportPromptBuilder.buildInstanceUserPrompt(
                instance, event.metricKey(), event.value(), event.severity(), event.collectedAt(), threshold);

        AnomalyReportContent content = claudeReportClient.generate(
                CLAUDE_MODEL, MAX_TOKENS, AnomalyReportPromptBuilder.SYSTEM_PROMPT, userPrompt,
                RESPONSE_SCHEMA, AnomalyReportContent.class);

        InstanceAnomalyReport report = InstanceAnomalyReport.builder()
                .instance(instance)
                .metricKey(event.metricKey())
                .severity(event.severity())
                .value(event.value())
                .collectedAt(event.collectedAt())
                .summary(content.summary())
                .content(toJson(content))
                .build();

        instanceAnomalyReportRepository.save(report);
    }

    private String toJson(AnomalyReportContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JacksonException e) {
            log.error("이상탐지 리포트 JSON 직렬화 실패", e);
            throw new CustomException(ReportErrorCode.CLAUDE_RESPONSE_PARSE_FAILED);
        }
    }
}