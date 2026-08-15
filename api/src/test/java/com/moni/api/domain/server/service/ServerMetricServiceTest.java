package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.server.dto.response.ServerRealtimeMetricsResponse;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ServerMetricServiceTest {

    @InjectMocks
    private ServerMetricService serverMetricService;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    @Test
    @DisplayName("서버 실시간 메트릭 조회 성공")
    void getServerRealtimeMetrics_success() {
        // given
        Long serverId = 100L;
        LocalDateTime now = LocalDateTime.now();

        JvmMetric jvm1 = JvmMetric.builder()
                .jvmHeapUsedBytes(67108864L)
                .jvmHeapMaxBytes(1610612736L)
                .jvmOldGenUsedBytes(33554432L)
                .gcPauseSecondsSum(0.01)
                .processUptimeSeconds(12345.0)
                .build();

        ServerHttpEndpointMetric http1 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/servers")
                .method("POST")
                .status("201")
                .requestsCount(100L)
                .requestsSum(2.5)
                .requestsMax(0.05)
                .build();

        ServerHikariCpPoolMetric hikari1 = ServerHikariCpPoolMetric.builder()
                .poolName("HikariPool-1")
                .active(5)
                .idle(5)
                .max(10)
                .build();

        ServerExecutorMetric exec1 = ServerExecutorMetric.builder()
                .name("appExecutor")
                .active(3)
                .max(10)
                .queuedTasks(1)
                .queueRemaining(99)
                .build();

        ServerRealtimeMetric metric1 = ServerRealtimeMetric.builder()
                .id(1L)
                .serverId(serverId)
                .collectedAt(now.minusSeconds(10))
                .jvmMetric(jvm1)
                .httpEndpoints(List.of(http1))
                .hikaricpPools(List.of(hikari1))
                .executors(List.of(exec1))
                .build();

        ServerRealtimeMetric metric2 = ServerRealtimeMetric.builder()
                .id(2L)
                .serverId(serverId)
                .collectedAt(now)
                .jvmMetric(jvm1)
                .httpEndpoints(List.of(http1))
                .hikaricpPools(List.of(hikari1))
                .executors(List.of(exec1))
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(serverId), any(Pageable.class)))
                .willReturn(List.of(metric2, metric1));

        // when
        ServerRealtimeMetricsResponse response = serverMetricService.getServerRealtimeMetrics(serverId, 30);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrent()).isNotNull();
        assertThat(response.getCurrent().getCollectedAt()).isEqualTo(now);
        assertThat(response.getCurrent().getProcessUptimeSeconds()).isEqualTo(12345.0);
        assertThat(response.getCurrent().getHttpEndpoints()).hasSize(1);

        assertThat(response.getSeries()).hasSize(2);
        assertThat(response.getSeries().get(0).getCollectedAt()).isEqualTo(now.minusSeconds(10));
        assertThat(response.getSeries().get(1).getCollectedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("존재하지 않는 서버의 실시간 메트릭 조회 시 예외 발생")
    void getServerRealtimeMetrics_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        given(serverRepository.existsById(serverId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverMetricService.getServerRealtimeMetrics(serverId, 30))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("수납 메트릭 데이터가 없을 경우 empty response 반환")
    void getServerRealtimeMetrics_emptyMetrics_returnsEmptyResponse() {
        // given
        Long serverId = 100L;
        given(serverRepository.existsById(serverId)).willReturn(true);
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(serverId), any(Pageable.class)))
                .willReturn(Collections.emptyList());

        // when
        ServerRealtimeMetricsResponse response = serverMetricService.getServerRealtimeMetrics(serverId, 30);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrent()).isNull();
        assertThat(response.getSeries()).isEmpty();
    }
}
