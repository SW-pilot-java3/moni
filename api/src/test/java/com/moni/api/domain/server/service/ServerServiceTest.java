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
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.global.error.ErrorCode;
import com.moni.api.global.error.exception.CustomException;
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

    @Test
    @DisplayName("신규 서버 등록 성공")
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
}
