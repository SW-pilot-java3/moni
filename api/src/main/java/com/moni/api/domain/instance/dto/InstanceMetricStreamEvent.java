package com.moni.api.domain.instance.dto;

import java.time.LocalDateTime;

public record InstanceMetricStreamEvent(
        Long instanceId,
        LocalDateTime collectedAt,
        Double cpuUsagePct,
        Long memAvailableBytes
) {
}