package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @InjectMocks
    private ServerService serverService;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private InstanceService instanceService;

    @Mock
    private ServerThresholdRepository serverThresholdRepository;

    @Test
    @DisplayName("신규 서버 등록 성공 (기본 임계치 7개 자동 초기화)")
    void createServer_success() {
        // given
        Long instanceId = 10L;
        Instance mockInstance = Mockito.mock(Instance.class);
        given(mockInstance.getId()).willReturn(instanceId);

        ServerCreateRequest request = new ServerCreateRequest(instanceId, "User-Service", 8080);

        Server mockSavedServer = Server.builder()
                .instance(mockInstance)
                .name("User-Service")
                .port(8080)
                .status(ServerStatus.DISCONNECTED)
                .build();
        ReflectionTestUtils.setField(mockSavedServer, "id", 100L);

        given(instanceService.getInstanceById(instanceId)).willReturn(mockInstance);
        given(serverRepository.save(any(Server.class))).willReturn(mockSavedServer);

        // when
        ServerResponse response = serverService.createServer(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(100L);
        assertThat(response.getInstanceId()).isEqualTo(instanceId);
        assertThat(response.getName()).isEqualTo("User-Service");
        assertThat(response.getPort()).isEqualTo(8080);
        assertThat(response.getStatus()).isEqualTo(ServerStatus.DISCONNECTED);

        verify(instanceService).getInstanceById(instanceId);
        verify(serverRepository).save(any(Server.class));
        verify(serverThresholdRepository).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 인스턴스에 서버 등록 시 예외 발생")
    void createServer_instanceNotFound_throwsException() {
        // given
        Long instanceId = 999L;
        ServerCreateRequest request = new ServerCreateRequest(instanceId, "User-Service", 8080);

        given(instanceService.getInstanceById(instanceId))
                .willThrow(new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverService.createServer(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", InstanceErrorCode.INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("서버 정보 수정 성공")
    void updateServer_success() {
        // given
        Long serverId = 100L;
        Long instanceId = 10L;
        Instance mockInstance = Mockito.mock(Instance.class);
        given(mockInstance.getId()).willReturn(instanceId);

        Server existingServer = Server.builder()
                .instance(mockInstance)
                .name("User-Service")
                .port(8080)
                .status(ServerStatus.CONNECTED)
                .build();
        ReflectionTestUtils.setField(existingServer, "id", serverId);

        ServerUpdateRequest updateRequest = new ServerUpdateRequest("User-Service-Core", 8081);

        given(serverRepository.findById(serverId)).willReturn(Optional.of(existingServer));

        // when
        ServerResponse response = serverService.updateServer(serverId, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getName()).isEqualTo("User-Service-Core");
        assertThat(response.getPort()).isEqualTo(8081);

        verify(serverRepository).findById(serverId);
    }

    @Test
    @DisplayName("존재하지 않는 서버 정보 수정 시 예외 발생")
    void updateServer_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        ServerUpdateRequest updateRequest = new ServerUpdateRequest("User-Service-Core", 8081);

        given(serverRepository.findById(serverId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.updateServer(serverId, updateRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("서버 삭제 성공")
    void deleteServer_success() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);

        given(serverRepository.findById(serverId)).willReturn(Optional.of(mockServer));

        // when
        ServerDeleteResponse response = serverService.deleteServer(serverId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDeletedServerId()).isEqualTo(serverId);

        verify(serverRepository).findById(serverId);
        verify(serverRepository).delete(mockServer);
    }

    @Test
    @DisplayName("존재하지 않는 서버 삭제 시 예외 발생")
    void deleteServer_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;

        given(serverRepository.findById(serverId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.deleteServer(serverId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }
}
