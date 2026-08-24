package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.dto.response.ServerSummaryResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.domain.server.validator.ServerValidator;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.List;
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
    private ServerValidator serverValidator;

    @Mock
    private StatJvmRepository statJvmRepository;

    @Mock
    private StatHttpRepository statHttpRepository;

    @Mock
    private StatHikariCpRepository statHikariCpRepository;

    @Mock
    private StatThreadPoolRepository statThreadPoolRepository;

    @Mock
    private ServerThresholdRepository serverThresholdRepository;

    private final Long userId = 1L;

    @Test
    @DisplayName("인스턴스 하위 서버 통계 요약 목록 조회 성공")
    void getServerSummaryList_success() {
        // given
        Long instanceId = 10L;
        Instance mockInstance = Mockito.mock(Instance.class);

        Server server1 = Server.builder().instance(mockInstance).name("order-api").port(8080)
                .status(ServerStatus.CONNECTED).build();
        ReflectionTestUtils.setField(server1, "id", 100L);

        Server server2 = Server.builder().instance(mockInstance).name("payment-api").port(8081)
                .status(ServerStatus.CONNECTED).build();
        ReflectionTestUtils.setField(server2, "id", 101L);

        StatJvm statJvm = StatJvm.builder()
                .heapUsedAvg(1200L * 1024 * 1024)
                .heapUsedMax(2048L * 1024 * 1024)
                .gcPauseCountSum(5L)
                .gcPauseSecondsSum(0.25)
                .build();
        StatHttp statHttp = StatHttp.builder().rpsAvg(128.5).avgResTimeMs(32.0).errorRateAvg(0.08).build();
        StatHikariCp statHikari = StatHikariCp.builder().activePoolAvg(6.0).activePoolMax(10).pendingThreadsMax(0)
                .build();
        StatThreadPool statPool = StatThreadPool.builder().activeThreadsAvg(4.0).maxThreadsAvg(10.0).queuedTasksMax(2)
                .build();

        given(serverValidator.validateAndGetInstance(instanceId, userId)).willReturn(mockInstance);
        given(serverRepository.findByInstanceId(instanceId)).willReturn(List.of(server1, server2));

        given(statJvmRepository.findFirstByServerIdOrderByStatTimeDesc(100L)).willReturn(Optional.of(statJvm));
        given(statHttpRepository.findFirstByServerIdOrderByStatTimeDesc(100L)).willReturn(Optional.of(statHttp));
        given(statHikariCpRepository.findFirstByServerIdOrderByStatTimeDesc(100L)).willReturn(Optional.of(statHikari));
        given(statThreadPoolRepository.findFirstByServerIdOrderByStatTimeDesc(100L)).willReturn(Optional.of(statPool));

        given(statJvmRepository.findFirstByServerIdOrderByStatTimeDesc(101L)).willReturn(Optional.empty());
        given(statHttpRepository.findFirstByServerIdOrderByStatTimeDesc(101L)).willReturn(Optional.empty());
        given(statHikariCpRepository.findFirstByServerIdOrderByStatTimeDesc(101L)).willReturn(Optional.empty());
        given(statThreadPoolRepository.findFirstByServerIdOrderByStatTimeDesc(101L)).willReturn(Optional.empty());

        // when
        List<ServerSummaryResponse> response = serverService.getServerSummaryList(instanceId, userId);

        // then
        assertThat(response).hasSize(2);

        // 1번 서버: Stat 매핑 검증
        assertThat(response.get(0).getServerId()).isEqualTo(100L);
        assertThat(response.get(0).getName()).isEqualTo("order-api");
        assertThat(response.get(0).getJvm().getHeapUsedMB()).isEqualTo(1200L);
        assertThat(response.get(0).getJvm().getHeapUsedMaxMB()).isEqualTo(2048L);
        assertThat(response.get(0).getJvm().getAvgGcPauseMs()).isEqualTo(50.0);
        assertThat(response.get(0).getHttp().getRps()).isEqualTo(128.5);
        assertThat(response.get(0).getHikaricp().getActive()).isEqualTo(6);
        assertThat(response.get(0).getHikaricp().getActiveMax()).isEqualTo(10);
        assertThat(response.get(0).getExecutors().getQueuedTasks()).isEqualTo(2);

        // 2번 서버: Stat 비어있는 경우 빈 DTO 검증
        assertThat(response.get(1).getServerId()).isEqualTo(101L);
        assertThat(response.get(1).getName()).isEqualTo("payment-api");
        assertThat(response.get(1).getJvm().getHeapUsedMB()).isNull();

        verify(serverValidator).validateAndGetInstance(instanceId, userId);
        verify(serverRepository).findByInstanceId(instanceId);
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 인스턴스의 서버 통계 요약 목록 조회 시 예외 발생")
    void getServerSummaryList_instanceNotFound_throwsException() {
        // given
        Long instanceId = 999L;

        given(serverValidator.validateAndGetInstance(instanceId, userId))
                .willThrow(new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverService.getServerSummaryList(instanceId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", InstanceErrorCode.INSTANCE_NOT_FOUND);
    }

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

        given(serverValidator.validateAndGetInstance(instanceId, userId)).willReturn(mockInstance);
        given(serverRepository.save(any(Server.class))).willReturn(mockSavedServer);

        // when
        ServerResponse response = serverService.createServer(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(100L);
        assertThat(response.getInstanceId()).isEqualTo(instanceId);
        assertThat(response.getName()).isEqualTo("User-Service");
        assertThat(response.getPort()).isEqualTo(8080);
        assertThat(response.getStatus()).isEqualTo(ServerStatus.DISCONNECTED);

        verify(serverValidator).validateAndGetInstance(instanceId, userId);
        verify(serverRepository).save(any(Server.class));
        verify(serverThresholdRepository).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 인스턴스에 서버 등록 시 예외 발생")
    void createServer_instanceNotFound_throwsException() {
        // given
        Long instanceId = 999L;
        ServerCreateRequest request = new ServerCreateRequest(instanceId, "User-Service", 8080);

        given(serverValidator.validateAndGetInstance(instanceId, userId))
                .willThrow(new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverService.createServer(userId, request))
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

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(existingServer);

        // when
        ServerResponse response = serverService.updateServer(serverId, userId, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getName()).isEqualTo("User-Service-Core");
        assertThat(response.getPort()).isEqualTo(8081);

        verify(serverValidator).validateAndGetServer(serverId, userId);
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 서버 정보 수정 시 예외 발생")
    void updateServer_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        ServerUpdateRequest updateRequest = new ServerUpdateRequest("User-Service-Core", 8081);

        given(serverValidator.validateAndGetServer(serverId, userId))
                .willThrow(new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverService.updateServer(serverId, userId, updateRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("서버 삭제 성공")
    void deleteServer_success() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(mockServer);

        // when
        ServerDeleteResponse response = serverService.deleteServer(serverId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDeletedServerId()).isEqualTo(serverId);

        verify(serverValidator).validateAndGetServer(serverId, userId);
        verify(serverRepository).delete(mockServer);
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 서버 삭제 시 예외 발생")
    void deleteServer_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;

        given(serverValidator.validateAndGetServer(serverId, userId))
                .willThrow(new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverService.deleteServer(serverId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }
}
