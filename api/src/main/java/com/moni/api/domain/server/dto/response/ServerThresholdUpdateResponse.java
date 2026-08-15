package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerThresholdUpdateResponse {

    private Long serverId;
    private Integer updatedCount;
    private LocalDateTime updatedAt;

    public static ServerThresholdUpdateResponse of(Long serverId, Integer updatedCount, LocalDateTime updatedAt) {
        return ServerThresholdUpdateResponse.builder()
                .serverId(serverId)
                .updatedCount(updatedCount)
                .updatedAt(updatedAt)
                .build();
    }
}
