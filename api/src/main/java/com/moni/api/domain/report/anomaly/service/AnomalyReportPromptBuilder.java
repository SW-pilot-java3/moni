package com.moni.api.domain.report.anomaly.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.global.threshold.ThresholdSeverity;
import java.time.LocalDateTime;

/**
 * 임계치를 초과한 단일 지표 스냅샷을 Claude에 넘길 프롬프트 텍스트로 만든다.
 */
final class AnomalyReportPromptBuilder {

    private AnomalyReportPromptBuilder() {
    }

    static final String SYSTEM_PROMPT = """
            당신은 인프라 모니터링 시스템의 이상탐지 분석가입니다.
            주어진 지표가 임계치를 초과한 상황을 보고, 발생 가능한 원인과 대응 방안을
            한국어로 간결하게 분석해 지정된 JSON 스키마로만 응답하세요.
            주어진 수치를 근거로 삼되, 추측이나 근거 없는 단정은 하지 마세요.
            """;

    static String buildInstanceUserPrompt(Instance instance, MetricKey metricKey, double value,
            ThresholdSeverity severity, LocalDateTime collectedAt, InstanceThreshold threshold) {
        return "인스턴스: " + instance.getName() + " (id=" + instance.getId() + ")\n"
                + "발생 시각: " + collectedAt + "\n"
                + "지표: " + metricKey + "\n"
                + "측정값: " + value + "\n"
                + "심각도: " + severity + "\n"
                + "임계치: warning=" + threshold.getWarningVal() + ", critical=" + threshold.getCriticalVal() + "\n";
    }

    static String buildServerUserPrompt(Server server, ServerMetricKey metricKey, double value,
            ThresholdSeverity severity, LocalDateTime collectedAt, ServerThreshold threshold) {
        return "서버: " + server.getName() + " (id=" + server.getId() + ")\n"
                + "발생 시각: " + collectedAt + "\n"
                + "지표: " + metricKey + " (" + metricKey.getDescription() + ")\n"
                + "측정값: " + value + "\n"
                + "심각도: " + severity + "\n"
                + "임계치: warning=" + threshold.getWarningValue() + ", critical=" + threshold.getCriticalValue() + "\n";
    }
}
