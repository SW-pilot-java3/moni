package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiKeyRotateResponse {

    private Long apiKeyId;
    private Long serverId;
    private String newApiKey;
    private LocalDateTime createdAt;

    public static ApiKeyRotateResponse of(Long apiKeyId, Long serverId, String newRawApiKey, LocalDateTime createdAt) {
        return ApiKeyRotateResponse.builder()
                .apiKeyId(apiKeyId)
                .serverId(serverId)
                .newApiKey(newRawApiKey)
                .createdAt(createdAt)
                .build();
    }
}
