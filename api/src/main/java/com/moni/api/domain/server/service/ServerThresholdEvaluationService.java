package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.threshold.ThresholdSeverity;
import com.moni.api.global.threshold.ThresholdSeverityResolver;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ServerThresholdEvaluationService {

    private final ServerThresholdRepository serverThresholdRepository;
    private final ApplicationEventPublisher eventPublisher;

    void evaluate(Long serverId, LocalDateTime collectedAt, ServerRealtimeMetric realtimeMetric,
            JvmMetric previousJvmMetric) {
        JvmMetric jvmMetric = realtimeMetric.getJvmMetric();

        Double jvmHeapUsagePct = JvmHeapUsageCalculator.calculate(jvmMetric);
        Double jvmOldGenUsagePct = JvmOldGenUsageCalculator.calculate(jvmMetric);
        Double gcPauseTimeSeconds = GcPauseTimeCalculator.calculate(previousJvmMetric, jvmMetric);
        Double hikariPoolUsagePct = HikariPoolUsageCalculator.calculate(realtimeMetric.getHikaricpPools());
        Double threadPoolQueueUsagePct = ThreadPoolQueueUsageCalculator.calculate(realtimeMetric.getExecutors());

        Map<ServerMetricKey, ServerThreshold> thresholds = serverThresholdRepository
                .findByServerId(serverId).stream()
                .collect(Collectors.toMap(ServerThreshold::getMetricKey, Function.identity()));

        evaluateMetric(serverId, ServerMetricKey.JVM_HEAP_USAGE, jvmHeapUsagePct, thresholds, collectedAt);
        evaluateMetric(serverId, ServerMetricKey.JVM_OLD_GEN_USAGE, jvmOldGenUsagePct, thresholds, collectedAt);
        evaluateMetric(serverId, ServerMetricKey.GC_PAUSE_TIME, gcPauseTimeSeconds, thresholds, collectedAt);
        evaluateMetric(serverId, ServerMetricKey.HIKARICP_POOL_USAGE, hikariPoolUsagePct, thresholds, collectedAt);
        evaluateMetric(serverId, ServerMetricKey.THREADPOOL_QUEUE_USAGE, threadPoolQueueUsagePct, thresholds, collectedAt);
    }

    private void evaluateMetric(Long serverId, ServerMetricKey metricKey, Double value,
            Map<ServerMetricKey, ServerThreshold> thresholds, LocalDateTime collectedAt) {
        ServerThreshold threshold = thresholds.get(metricKey);
        if (threshold == null) {
            return;
        }

        ThresholdSeverity severity = ThresholdSeverityResolver.resolve(
                value, threshold.getWarningValue(), threshold.getCriticalValue());
        if (severity == null) {
            return;
        }

        eventPublisher.publishEvent(
                new ServerMetricThresholdExceededEvent(serverId, metricKey, value, severity, collectedAt));
    }
}
