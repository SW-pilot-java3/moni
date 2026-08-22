package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.response.ApiKeyCreateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyRotateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyStatusResponse;
import com.moni.api.domain.server.entity.ApiKey;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ApiKeyRepository;
import com.moni.api.domain.server.util.ApiKeyGenerator;
import com.moni.api.domain.server.validator.ServerValidator;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerApiKeyService {

    private final ServerValidator serverValidator;
    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public ApiKeyCreateResponse createApiKey(Long serverId, Long userId) {
        Server server = serverValidator.validateAndGetServer(serverId, userId);

        apiKeyRepository.findByServerIdAndRevokedAtIsNull(serverId).ifPresent(key -> {
            throw new CustomException(ServerErrorCode.API_KEY_ALREADY_EXISTS);
        });

        String rawApiKey = ApiKeyGenerator.generateApiKey();
        String keyHash = ApiKeyGenerator.hash(rawApiKey);

        ApiKey apiKey = ApiKey.builder()
                .server(server)
                .keyHash(keyHash)
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        return ApiKeyCreateResponse.of(savedApiKey.getId(), serverId, rawApiKey, savedApiKey.getCreatedAt());
    }

    @Transactional
    public ApiKeyRotateResponse rotateApiKey(Long serverId, Long userId) {
        Server server = serverValidator.validateAndGetServer(serverId, userId);

        apiKeyRepository.findByServerIdAndRevokedAtIsNull(serverId).ifPresent(ApiKey::revoke);

        String newRawApiKey = ApiKeyGenerator.generateApiKey();
        String keyHash = ApiKeyGenerator.hash(newRawApiKey);

        ApiKey newApiKey = ApiKey.builder()
                .server(server)
                .keyHash(keyHash)
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(newApiKey);

        return ApiKeyRotateResponse.of(savedApiKey.getId(), serverId, newRawApiKey, savedApiKey.getCreatedAt());
    }

    public ApiKeyStatusResponse getApiKeyStatus(Long serverId, Long userId) {
        serverValidator.validateAndGetServer(serverId, userId);

        return apiKeyRepository.findFirstByServerIdOrderByCreatedAtDesc(serverId)
                .map(key -> ApiKeyStatusResponse.of(key.isActive(), key.getCreatedAt(), key.getRevokedAt()))
                .orElseGet(() -> ApiKeyStatusResponse.of(false, null, null));
    }

    public Server authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new CustomException(ServerErrorCode.INVALID_API_KEY);
        }

        String keyHash = ApiKeyGenerator.hash(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash)
                .orElseThrow(() -> new CustomException(ServerErrorCode.INVALID_API_KEY));

        Server server = apiKey.getServer();
        if (server == null) {
            throw new CustomException(ServerErrorCode.SERVER_NOT_FOUND);
        }

        return server;
    }
}
