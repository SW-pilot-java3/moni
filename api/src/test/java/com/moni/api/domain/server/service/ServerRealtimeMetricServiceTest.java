package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.server.dto.response.ServerSseStreamResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerRealtimeMetricServiceTest {

    @InjectMocks
    private ServerRealtimeMetricService serverRealtimeMetricService;

    @Mock
    private ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    @Mock
    private ServerSseService serverSseService;

    private Server server;
    private LocalDateTime collectedAt;

    @BeforeEach
    void setUp() throws Exception {
        Instance instance = Instance.builder().name("TestInstance").build();
        setEntityId(instance, 10L);

        server = Server.builder()
                .instance(instance)
                .name("TestServer")
                .port(8080)
                .status(ServerStatus.DISCONNECTED)
                .build();
        setEntityId(server, 1L);

        collectedAt = LocalDateTime.of(2026, 8, 19, 12, 0, 0);
    }

    private void setEntityId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Test
    @DisplayName("서버 메트릭 기록 및 SSE 브로드캐스트 성공")
    void recordServerMetric_Success() {
        // given
        MetricRecordRequest.HttpEndpointPayload httpEndpoint = MetricRecordRequest.HttpEndpointPayload.builder()
                .uri("/api/v1/test")
                .method("GET")
                .status("200")
                .requestsCount(100L)
                .requestsSum(2.5)
                .requestsMax(0.1)
                .build();

        MetricRecordRequest.HikariPoolPayload hikariPool = MetricRecordRequest.HikariPoolPayload.builder()
                .poolName("HikariPool-1")
                .active(5)
                .idle(5)
                .pending(0)
                .max(10)
                .timeoutsTotal(0L)
                .build();

        MetricRecordRequest.ExecutorPayload executor = MetricRecordRequest.ExecutorPayload.builder()
                .name("taskExecutor")
                .active(3)
                .max(10)
                .queuedTasks(2)
                .queueRemaining(98)
                .build();

        MetricRecordRequest.ServerMetricPayload payload = MetricRecordRequest.ServerMetricPayload.builder()
                .jvmHeapUsedBytes(500_000_000L)
                .jvmHeapMaxBytes(1_000_000_000L)
                .jvmOldGenUsedBytes(200_000_000L)
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(0.5)
                .processUptimeSeconds(3600.0)
                .jvmThreadsLive(50)
                .jvmThreadsBlocked(1)
                .httpEndpoints(List.of(httpEndpoint))
                .hikaricpPools(List.of(hikariPool))
                .executors(List.of(executor))
                .build();

        // when
        serverRealtimeMetricService.recordServerMetric(server, collectedAt, payload);

        // then - DB 저장 검증
        ArgumentCaptor<ServerRealtimeMetric> metricCaptor = ArgumentCaptor.forClass(ServerRealtimeMetric.class);
        verify(serverRealtimeMetricRepository).save(metricCaptor.capture());
        ServerRealtimeMetric saved = metricCaptor.getValue();
        assertThat(saved.getServerId()).isEqualTo(1L);
        assertThat(saved.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(saved.getJvmMetric().getJvmHeapUsedBytes()).isEqualTo(500_000_000L);
        assertThat(saved.getHttpEndpoints()).hasSize(1);
        assertThat(saved.getHikaricpPools()).hasSize(1);
        assertThat(saved.getExecutors()).hasSize(1);

        // then - 서버 상태 및 수신 일시 갱신 검증
        assertThat(server.getStatus()).isEqualTo(ServerStatus.CONNECTED);
        assertThat(server.getLastReceivedAt()).isEqualTo(collectedAt);

        // then - SSE 브로드캐스트 검증
        ArgumentCaptor<ServerSseStreamResponse> sseCaptor = ArgumentCaptor.forClass(ServerSseStreamResponse.class);
        verify(serverSseService).broadcastServerMetric(eq(1L), sseCaptor.capture());
        ServerSseStreamResponse sseResponse = sseCaptor.getValue();
        assertThat(sseResponse.getServerId()).isEqualTo(1L);
        assertThat(sseResponse.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(sseResponse.getSummary().getHikaricpActiveTotal()).isEqualTo(5);
        assertThat(sseResponse.getSummary().getHikaricpMaxTotal()).isEqualTo(10);
        assertThat(sseResponse.getSummary().getExecutorActiveTotal()).isEqualTo(3);
        assertThat(sseResponse.getSummary().getExecutorMaxTotal()).isEqualTo(10);
        assertThat(sseResponse.getSummary().getAvgLatencyMs()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("HTTP 엔드포인트 그룹핑 및 에러율 계산 검증")
    void recordServerMetric_HttpEndpointGroupingAndErrorRate() {
        // given
        MetricRecordRequest.HttpEndpointPayload successEp = MetricRecordRequest.HttpEndpointPayload.builder()
                .uri("/api/v1/users")
                .method("GET")
                .status("200")
                .requestsCount(90L)
                .requestsSum(1.8)
                .requestsMax(0.05)
                .build();

        MetricRecordRequest.HttpEndpointPayload errorEp = MetricRecordRequest.HttpEndpointPayload.builder()
                .uri("/api/v1/users")
                .method("GET")
                .status("500")
                .requestsCount(10L)
                .requestsSum(0.7)
                .requestsMax(0.15)
                .build();

        MetricRecordRequest.ServerMetricPayload payload = MetricRecordRequest.ServerMetricPayload.builder()
                .httpEndpoints(List.of(successEp, errorEp))
                .build();

        // when
        serverRealtimeMetricService.recordServerMetric(server, collectedAt, payload);

        // then - DB에는 원본 2건 모두 저장
        ArgumentCaptor<ServerRealtimeMetric> metricCaptor = ArgumentCaptor.forClass(ServerRealtimeMetric.class);
        verify(serverRealtimeMetricRepository).save(metricCaptor.capture());
        assertThat(metricCaptor.getValue().getHttpEndpoints()).hasSize(2);

        // then - SSE DTO에는 1개 그룹으로 집계되어 10.0% 에러율 산출
        ArgumentCaptor<ServerSseStreamResponse> sseCaptor = ArgumentCaptor.forClass(ServerSseStreamResponse.class);
        verify(serverSseService).broadcastServerMetric(eq(1L), sseCaptor.capture());
        ServerSseStreamResponse sseResponse = sseCaptor.getValue();
        assertThat(sseResponse.getHttpEndpoints()).hasSize(1);

        ServerSseStreamResponse.ServerHttpMetricsDto groupedEp = sseResponse.getHttpEndpoints().get(0);
        assertThat(groupedEp.getUri()).isEqualTo("/api/v1/users");
        assertThat(groupedEp.getMethod()).isEqualTo("GET");
        assertThat(groupedEp.getRequestsCount()).isEqualTo(100L); // 90 + 10
        assertThat(groupedEp.getErrorRatePct()).isEqualTo(10.0); // 10 / 100 * 100 = 10.0%
        assertThat(groupedEp.getAvgLatencyMs()).isEqualTo(25.0); // (1.8 + 0.7) / 100 * 1000 = 25.0ms
        assertThat(groupedEp.getMaxLatencyMs()).isEqualTo(150.0); // 0.15 * 1000 = 150.0ms
    }

    @Test
    @DisplayName("실시간 RPS 계산 검증")
    void recordServerMetric_RealtimeRpsCalculation() {
        // given - 1차 수집 (12:00:00, requestsCount: 100)
        MetricRecordRequest.HttpEndpointPayload ep1 = MetricRecordRequest.HttpEndpointPayload.builder()
                .uri("/api/v1/items")
                .method("GET")
                .status("200")
                .requestsCount(100L)
                .requestsSum(2.0)
                .requestsMax(0.05)
                .build();

        MetricRecordRequest.ServerMetricPayload payload1 = MetricRecordRequest.ServerMetricPayload.builder()
                .httpEndpoints(List.of(ep1))
                .build();

        serverRealtimeMetricService.recordServerMetric(server, collectedAt, payload1);

        // given - 2차 수집 (12:00:10, 10초 경과, requestsCount: 200 -> diff 100건 / 10초 = 10.0 RPS)
        LocalDateTime collectedAt2 = collectedAt.plusSeconds(10);
        MetricRecordRequest.HttpEndpointPayload ep2 = MetricRecordRequest.HttpEndpointPayload.builder()
                .uri("/api/v1/items")
                .method("GET")
                .status("200")
                .requestsCount(200L)
                .requestsSum(4.0)
                .requestsMax(0.05)
                .build();

        MetricRecordRequest.ServerMetricPayload payload2 = MetricRecordRequest.ServerMetricPayload.builder()
                .httpEndpoints(List.of(ep2))
                .build();

        // when
        serverRealtimeMetricService.recordServerMetric(server, collectedAt2, payload2);

        // then
        ArgumentCaptor<ServerSseStreamResponse> sseCaptor = ArgumentCaptor.forClass(ServerSseStreamResponse.class);
        verify(serverSseService, org.mockito.Mockito.times(2)).broadcastServerMetric(eq(1L), sseCaptor.capture());
        ServerSseStreamResponse sseResponse2 = sseCaptor.getAllValues().get(1);

        assertThat(sseResponse2.getHttpEndpoints().get(0).getRps()).isEqualTo(10.0);
        assertThat(sseResponse2.getSummary().getTotalRps()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("서버 또는 페이로드가 null인 경우 무시")
    void recordServerMetric_NullServerOrPayload() {
        // when
        serverRealtimeMetricService.recordServerMetric(null, collectedAt, null);

        // then
        verify(serverRealtimeMetricRepository, never()).save(any());
        verify(serverSseService, never()).broadcastServerMetric(any(), any());
    }
}
