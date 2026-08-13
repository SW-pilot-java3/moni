package com.moni.api.domain.instance.dto;

import java.time.LocalDateTime;

public record InstanceThresholdUpdateResponse(
        Long instanceId,
        int updatedCount,
        LocalDateTime updatedAt
) {
}