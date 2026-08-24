package com.moni.api.domain.report.anomaly.service;

import com.moni.api.domain.report.anomaly.dto.AnomalyReportContent;
import com.moni.api.domain.report.anomaly.entity.ServerAnomalyReport;
import com.moni.api.domain.report.anomaly.repository.ServerAnomalyReportRepository;
import com.moni.api.domain.report.client.ClaudeReportClient;
import com.moni.api.domain.report.exception.ReportErrorCode;
import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import com.moni.api.global.threshold.CooldownPolicy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class ServerAnomalyReportService {

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

    private final ServerRepository serverRepository;
    private final ServerThresholdRepository serverThresholdRepository;
    private final ServerAnomalyReportRepository serverAnomalyReportRepository;
    private final ClaudeReportClient claudeReportClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void generateReport(ServerMetricThresholdExceededEvent event) {
        Optional<ServerAnomalyReport> lastReport = serverAnomalyReportRepository
                .findFirstByServerIdAndMetricKeyOrderByCollectedAtDesc(event.serverId(), event.metricKey());
        if (lastReport.isPresent() && CooldownPolicy.shouldSuppress(
                lastReport.get().getSeverity(), lastReport.get().getCollectedAt(),
                event.severity(), event.collectedAt())) {
            log.info("쿨다운 중 - 이상탐지 리포트 생성 스킵 - serverId={}, metricKey={}",
                    event.serverId(), event.metricKey());
            return;
        }

        Server server = serverRepository.findById(event.serverId())
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));
        ServerThreshold threshold = serverThresholdRepository
                .findByServerIdAndMetricKey(event.serverId(), event.metricKey())
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_THRESHOLD_NOT_FOUND));

        String userPrompt = AnomalyReportPromptBuilder.buildServerUserPrompt(
                server, event.metricKey(), event.value(), event.severity(), event.collectedAt(), threshold);

        AnomalyReportContent content = claudeReportClient.generate(
                CLAUDE_MODEL, MAX_TOKENS, AnomalyReportPromptBuilder.SYSTEM_PROMPT, userPrompt,
                RESPONSE_SCHEMA, AnomalyReportContent.class);

        ServerAnomalyReport report = ServerAnomalyReport.builder()
                .server(server)
                .metricKey(event.metricKey())
                .severity(event.severity())
                .value(event.value())
                .collectedAt(event.collectedAt())
                .summary(content.summary())
                .content(toJson(content))
                .build();

        serverAnomalyReportRepository.save(report);
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
