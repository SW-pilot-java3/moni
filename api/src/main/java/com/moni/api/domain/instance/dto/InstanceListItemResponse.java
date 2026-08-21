package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.enums.InstanceStatus;

import java.time.LocalDateTime;

public record InstanceListItemResponse(
        Long instanceId,
        String name,
        String ip,
        InstanceStatus status,
        long serverCount,
        LocalDateTime lastReceivedAt,
        LocalDateTime createdAt
) {
    public static InstanceListItemResponse of(Instance instance, long serverCount) {
        return new InstanceListItemResponse(
                instance.getId(),
                instance.getName(),
                instance.getIp(),
                instance.getStatus(),
                serverCount,
                instance.getLastReceivedAt(),
                instance.getCreatedAt()
        );
    }
}