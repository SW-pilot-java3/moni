package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiKeyCreateResponse {

    private Long apiKeyId;
    private Long serverId;
    private String apiKey;
    private LocalDateTime createdAt;

    public static ApiKeyCreateResponse of(Long apiKeyId, Long serverId, String rawApiKey, LocalDateTime createdAt) {
        return ApiKeyCreateResponse.builder()
                .apiKeyId(apiKeyId)
                .serverId(serverId)
                .apiKey(rawApiKey)
                .createdAt(createdAt)
                .build();
    }
}
