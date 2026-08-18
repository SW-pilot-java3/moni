package com.moni.api.domain.instance.dto;

import java.time.LocalDateTime;

public record InstanceRealtimeMetricResponse(
        LocalDateTime collectedAt,
        Double cpuUsagePct,
        Long memAvailableBytes
) {
}