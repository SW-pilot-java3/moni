package com.moni.api.domain.server.dto;

import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.global.threshold.ThresholdSeverity;
import java.time.LocalDateTime;

public record ServerMetricThresholdExceededEvent(
        Long serverId,
        ServerMetricKey metricKey,
        double value,
        ThresholdSeverity severity,
        LocalDateTime collectedAt
) {
}
