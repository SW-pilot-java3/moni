package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.global.threshold.ThresholdSeverity;
import java.time.LocalDateTime;

public record InstanceMetricThresholdExceededEvent(
        Long instanceId,
        MetricKey metricKey,
        double value,
        ThresholdSeverity severity,
        LocalDateTime collectedAt
) {
}
