package com.moni.api.domain.metric.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.dto.InstanceRealtimeMetricCreateRequest;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceRealtimeMetricService;
import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.metric.dto.response.MetricRecordResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.service.ServerApiKeyService;
import com.moni.api.domain.server.service.ServerRealtimeMetricService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @InjectMocks
    private MetricService metricService;

    @Mock
    private ServerApiKeyService serverApiKeyService;

    @Mock
    private InstanceRealtimeMetricService instanceRealtimeMetricService;

    @Mock
    private ServerRealtimeMetricService serverRealtimeMetricService;

    private Server server;
    private final String rawApiKey = "test-api-key-1234";

    @BeforeEach
    void setUp() throws Exception {
        Instance instance = Instance.builder().name("TestInstance").build();
        setEntityId(instance, 10L);

        server = Server.builder()
                .instance(instance)
                .name("TestServer")
                .port(8080)
                .build();
        setEntityId(server, 1L);
    }

    private void setEntityId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Test
    @DisplayName("메트릭 기록 및 도메인 위임 호출 성공")
    void recordMetrics_Success() {
        // given
        given(serverApiKeyService.authenticate(rawApiKey)).willReturn(server);

        MetricRecordRequest.InstanceMetricPayload instancePayload = MetricRecordRequest.InstanceMetricPayload.builder()
                .cpuSecondsTotal(10.0)
                .cpuIdleSecondsTotal(90.0)
                .cpuIowaitSecondsTotal(0.0)
                .memTotalBytes(16_000_000_000L)
                .memFreeBytes(8_000_000_000L)
                .memAvailableBytes(10_000_000_000L)
                .buffersBytes(1_000_000_000L)
                .cachedBytes(2_000_000_000L)
                .swapTotalBytes(0L)
                .swapFreeBytes(0L)
                .cpus(List.of())
                .disks(List.of())
                .filesystems(List.of())
                .networks(List.of())
                .build();

        MetricRecordRequest.ServerMetricPayload serverPayload = MetricRecordRequest.ServerMetricPayload.builder()
                .jvmHeapUsedBytes(500_000_000L)
                .jvmHeapMaxBytes(1_000_000_000L)
                .build();

        MetricRecordRequest request = MetricRecordRequest.builder()
                .collectedAt(Instant.parse("2026-08-19T12:00:00Z"))
                .instance(instancePayload)
                .server(serverPayload)
                .build();

        // when
        MetricRecordResponse response = metricService.recordMetrics(rawApiKey, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getReceivedAt()).isNotNull();

        verify(serverApiKeyService).authenticate(rawApiKey);
        verify(instanceRealtimeMetricService).recordMetric(eq(10L), any(InstanceRealtimeMetricCreateRequest.class));
        verify(serverRealtimeMetricService).recordServerMetric(eq(server), any(), eq(serverPayload));
    }
}
