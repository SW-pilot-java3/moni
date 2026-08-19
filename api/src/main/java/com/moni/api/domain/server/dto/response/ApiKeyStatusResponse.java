package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiKeyStatusResponse {

    private Boolean hasApiKey;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    public static ApiKeyStatusResponse of(Boolean hasApiKey, LocalDateTime createdAt, LocalDateTime revokedAt) {
        return ApiKeyStatusResponse.builder()
                .hasApiKey(hasApiKey)
                .createdAt(createdAt)
                .revokedAt(revokedAt)
                .build();
    }
}
