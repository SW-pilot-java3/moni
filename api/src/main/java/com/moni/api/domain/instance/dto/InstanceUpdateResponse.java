package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.enums.InstanceStatus;

import java.time.LocalDateTime;

public record InstanceUpdateResponse(
        Long instanceId,
        String name,
        String ip,
        InstanceStatus status,
        LocalDateTime updatedAt
) {
    public static InstanceUpdateResponse from(Instance instance) {
        return new InstanceUpdateResponse(
                instance.getId(),
                instance.getName(),
                instance.getIp(),
                instance.getStatus(),
                instance.getUpdatedAt()
        );
    }
}