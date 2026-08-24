package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.server.dto.response.ApiKeyCreateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyRotateResponse;
import com.moni.api.domain.server.dto.response.ApiKeyStatusResponse;
import com.moni.api.domain.server.entity.ApiKey;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ApiKeyRepository;
import com.moni.api.domain.server.util.ApiKeyGenerator;
import com.moni.api.domain.server.validator.ServerValidator;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServerApiKeyServiceTest {

    @InjectMocks
    private ServerApiKeyService serverApiKeyService;

    @Mock
    private ServerValidator serverValidator;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private final Long userId = 1L;

    @Test
    @DisplayName("API Key 최초 발급 성공")
    void createApiKey_success() {
        // given
        Long serverId = 1L;
        Server server = Server.builder()
                .name("User-Service")
                .port(8080)
                .status(ServerStatus.CONNECTED)
                .build();

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(server);
        given(apiKeyRepository.findByServerIdAndRevokedAtIsNull(serverId)).willReturn(Optional.empty());
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            ReflectionTestUtils.setField(key, "id", 1L);
            ReflectionTestUtils.setField(key, "createdAt", LocalDateTime.now());
            return key;
        });

        // when
        ApiKeyCreateResponse response = serverApiKeyService.createApiKey(serverId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getApiKey()).isNotBlank();
        verify(serverValidator).validateAndGetServer(serverId, userId);
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API Key 최초 발급 실패 - 서버 없음 또는 권한 없음")
    void createApiKey_serverNotFound() {
        // given
        Long serverId = 999L;
        given(serverValidator.validateAndGetServer(serverId, userId))
                .willThrow(new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverApiKeyService.createApiKey(serverId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("API Key 최초 발급 실패 - 이미 활성 키 존재")
    void createApiKey_alreadyExists() {
        // given
        Long serverId = 1L;
        Server server = Server.builder().name("User-Service").port(8080).build();
        ApiKey existingKey = ApiKey.builder().server(server).keyHash("hash").build();

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(server);
        given(apiKeyRepository.findByServerIdAndRevokedAtIsNull(serverId)).willReturn(Optional.of(existingKey));

        // when & then
        assertThatThrownBy(() -> serverApiKeyService.createApiKey(serverId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.API_KEY_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("API Key 재발급(Rotate) 성공 - 기존 키 만료 후 새 키 발급")
    void rotateApiKey_success() {
        // given
        Long serverId = 1L;
        Server server = Server.builder().name("User-Service").port(8080).build();
        ApiKey oldKey = ApiKey.builder().server(server).keyHash("old_hash").build();

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(server);
        given(apiKeyRepository.findByServerIdAndRevokedAtIsNull(serverId)).willReturn(Optional.of(oldKey));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            ReflectionTestUtils.setField(key, "id", 2L);
            ReflectionTestUtils.setField(key, "createdAt", LocalDateTime.now());
            return key;
        });

        // when
        ApiKeyRotateResponse response = serverApiKeyService.rotateApiKey(serverId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getNewApiKey()).isNotBlank();
        assertThat(oldKey.getRevokedAt()).isNotNull();
        verify(serverValidator).validateAndGetServer(serverId, userId);
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API Key 상태 조회 성공 - 활성 키 존재")
    void getApiKeyStatus_activeKey_success() {
        // given
        Long serverId = 1L;
        Server server = Server.builder().name("User-Service").port(8080).build();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        ApiKey activeKey = ApiKey.builder()
                .server(server)
                .keyHash("hash")
                .build();
        ReflectionTestUtils.setField(activeKey, "createdAt", createdAt);

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(server);
        given(apiKeyRepository.findFirstByServerIdOrderByCreatedAtDesc(serverId)).willReturn(Optional.of(activeKey));

        // when
        ApiKeyStatusResponse response = serverApiKeyService.getApiKeyStatus(serverId, userId);

        // then
        assertThat(response.getHasApiKey()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getRevokedAt()).isNull();
        verify(serverValidator).validateAndGetServer(serverId, userId);
    }

    @Test
    @DisplayName("API Key 상태 조회 성공 - 키 없음")
    void getApiKeyStatus_noKey_success() {
        // given
        Long serverId = 1L;
        Server server = Server.builder().name("User-Service").port(8080).build();
        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(server);
        given(apiKeyRepository.findFirstByServerIdOrderByCreatedAtDesc(serverId)).willReturn(Optional.empty());

        // when
        ApiKeyStatusResponse response = serverApiKeyService.getApiKeyStatus(serverId, userId);

        // then
        assertThat(response.getHasApiKey()).isFalse();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getRevokedAt()).isNull();
        verify(serverValidator).validateAndGetServer(serverId, userId);
    }

    @Test
    @DisplayName("API Key 인증 성공 - 유효한 키 전달 시 Server 반환")
    void authenticate_success() {
        // given
        String rawApiKey = "valid_api_key_12345";
        String keyHash = ApiKeyGenerator.hash(rawApiKey);
        Server server = Server.builder().name("Auth-Server").port(8080).build();
        ApiKey apiKey = ApiKey.builder().server(server).keyHash(keyHash).build();

        given(apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash)).willReturn(Optional.of(apiKey));

        // when
        Server result = serverApiKeyService.authenticate(rawApiKey);

        // then
        assertThat(result).isEqualTo(server);
    }

    @Test
    @DisplayName("API Key 인증 실패 - 유효하지 않거나 폐기된 키")
    void authenticate_invalidKey() {
        // given
        String rawApiKey = "invalid_key";
        String keyHash = ApiKeyGenerator.hash(rawApiKey);

        given(apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverApiKeyService.authenticate(rawApiKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.INVALID_API_KEY);
    }

    @Test
    @DisplayName("API Key 인증 실패 - null 또는 공백 키")
    void authenticate_nullOrBlankKey() {
        // when & then
        assertThatThrownBy(() -> serverApiKeyService.authenticate(null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.INVALID_API_KEY);

        assertThatThrownBy(() -> serverApiKeyService.authenticate("   "))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.INVALID_API_KEY);
    }
}
