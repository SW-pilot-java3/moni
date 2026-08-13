package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.enums.InstanceStatus;

import java.time.LocalDateTime;

public record InstanceCreateResponse(
        Long instanceId,
        String name,
        String ip,
        InstanceStatus status,
        LocalDateTime lastReceivedAt,
        LocalDateTime createdAt
) {
    public static InstanceCreateResponse from(Instance instance) {
        return new InstanceCreateResponse(
                instance.getId(),
                instance.getName(),
                instance.getIp(),
                instance.getStatus(),
                instance.getLastReceivedAt(),
                instance.getCreatedAt()
        );
    }
}