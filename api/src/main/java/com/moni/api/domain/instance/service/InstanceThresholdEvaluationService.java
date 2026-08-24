package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.global.threshold.ThresholdSeverity;
import com.moni.api.global.threshold.ThresholdSeverityResolver;
import java.time.LocalDateTime;
import java.util.List;
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
class InstanceThresholdEvaluationService {

    private final InstanceThresholdRepository instanceThresholdRepository;
    private final InstanceAnomalyDebounceEvaluator instanceAnomalyDebounceEvaluator;
    private final ApplicationEventPublisher eventPublisher;

    void evaluate(Long instanceId, LocalDateTime collectedAt, Double cpuUsagePct,
            MemoryMetrics memoryMetrics, List<InstanceFileSystemMetric> filesystems,
            Double diskLatencyMs, Double netErrorRate) {
        Double memUsagePct = MemoryUsageCalculator.calculate(memoryMetrics);
        Double diskUsagePct = DiskUsageCalculator.calculate(filesystems);

        Map<MetricKey, InstanceThreshold> thresholds = instanceThresholdRepository
                .findAllByInstanceId(instanceId).stream()
                .collect(Collectors.toMap(InstanceThreshold::getMetricKey, Function.identity()));

        evaluateMetric(instanceId, MetricKey.CPU_USAGE, cpuUsagePct, thresholds, collectedAt);
        evaluateMetric(instanceId, MetricKey.MEM_USAGE, memUsagePct, thresholds, collectedAt);
        evaluateMetric(instanceId, MetricKey.DISK_USAGE, diskUsagePct, thresholds, collectedAt);
        evaluateMetric(instanceId, MetricKey.DISK_LATENCY, diskLatencyMs, thresholds, collectedAt);
        evaluateMetric(instanceId, MetricKey.NET_ERROR_RATE, netErrorRate, thresholds, collectedAt);
    }

    private void evaluateMetric(Long instanceId, MetricKey metricKey, Double value,
            Map<MetricKey, InstanceThreshold> thresholds, LocalDateTime collectedAt) {
        InstanceThreshold threshold = thresholds.get(metricKey);
        if (threshold == null) {
            return;
        }

        ThresholdSeverity severity = ThresholdSeverityResolver.resolve(
                value, threshold.getWarningVal(), threshold.getCriticalVal());
        if (severity == null) {
            return;
        }

        if (!instanceAnomalyDebounceEvaluator.isConsecutivelyExceeded(instanceId, metricKey, threshold)) {
            return;
        }

        eventPublisher.publishEvent(
                new InstanceMetricThresholdExceededEvent(instanceId, metricKey, value, severity, collectedAt));
    }
}
