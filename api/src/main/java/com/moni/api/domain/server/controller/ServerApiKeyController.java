package com.moni.api.domain.server.controller;

import com.moni.api.domain.server.dto.response.ApiKeyCreateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyRotateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyStatusResponse;
import com.moni.api.domain.server.service.ServerApiKeyService;
import com.moni.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "서버 API 키")
@RestController
@RequestMapping("/api/v1/servers/{serverId}/api-keys")
@RequiredArgsConstructor
public class ServerApiKeyController {

    private final ServerApiKeyService serverApiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyCreateResponse>> createApiKey(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        ApiKeyCreateResponse response = serverApiKeyService.createApiKey(serverId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/rotate")
    public ResponseEntity<ApiResponse<ApiKeyRotateResponse>> rotateApiKey(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        ApiKeyRotateResponse response = serverApiKeyService.rotateApiKey(serverId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ApiKeyStatusResponse>> getApiKeyStatus(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        ApiKeyStatusResponse response = serverApiKeyService.getApiKeyStatus(serverId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
