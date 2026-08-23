package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.threshold.ThresholdSeverity;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ServerThresholdEvaluationServiceTest {

    @InjectMocks
    private ServerThresholdEvaluationService serverThresholdEvaluationService;

    @Mock
    private ServerThresholdRepository serverThresholdRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("JVM_HEAP_USAGE가 critical을 넘으면 CRITICAL 이벤트를 발행한다")
    void evaluate_jvmHeapUsageAboveCritical_publishesCriticalEvent() {
        // given
        Long serverId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        ServerThreshold heapThreshold = ServerThreshold.builder()
                .metricKey(ServerMetricKey.JVM_HEAP_USAGE)
                .warningValue(80.0)
                .criticalValue(90.0)
                .build();
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(950_000_000L)
                .jvmHeapMaxBytes(1_000_000_000L) // 사용률 95%
                .build();
        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetric.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .jvmMetric(jvmMetric)
                .build();

        given(serverThresholdRepository.findByServerId(serverId)).willReturn(List.of(heapThreshold));

        // when
        serverThresholdEvaluationService.evaluate(serverId, collectedAt, realtimeMetric, null);

        // then
        ArgumentCaptor<ServerMetricThresholdExceededEvent> captor =
                ArgumentCaptor.forClass(ServerMetricThresholdExceededEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        ServerMetricThresholdExceededEvent event = captor.getValue();
        assertThat(event.serverId()).isEqualTo(serverId);
        assertThat(event.metricKey()).isEqualTo(ServerMetricKey.JVM_HEAP_USAGE);
        assertThat(event.severity()).isEqualTo(ThresholdSeverity.CRITICAL);
    }

    @Test
    @DisplayName("GC_PAUSE_TIME이 warning 이상이면 WARNING 이벤트를 발행한다 (이전 레코드와의 델타)")
    void evaluate_gcPauseTimeWarningRange_publishesWarningEvent() {
        // given
        Long serverId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        ServerThreshold gcThreshold = ServerThreshold.builder()
                .metricKey(ServerMetricKey.GC_PAUSE_TIME)
                .warningValue(0.5)
                .criticalValue(1.0)
                .build();
        JvmMetric previousJvmMetric = JvmMetric.builder()
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(1.0)
                .build();
        JvmMetric currentJvmMetric = JvmMetric.builder()
                .gcPauseSecondsCount(12L) // countDelta = 2
                .gcPauseSecondsSum(2.6) // sumDelta = 1.6 -> 평균 0.8초
                .build();
        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetric.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .jvmMetric(currentJvmMetric)
                .build();

        given(serverThresholdRepository.findByServerId(serverId)).willReturn(List.of(gcThreshold));

        // when
        serverThresholdEvaluationService.evaluate(serverId, collectedAt, realtimeMetric, previousJvmMetric);

        // then
        ArgumentCaptor<ServerMetricThresholdExceededEvent> captor =
                ArgumentCaptor.forClass(ServerMetricThresholdExceededEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().metricKey()).isEqualTo(ServerMetricKey.GC_PAUSE_TIME);
        assertThat(captor.getValue().severity()).isEqualTo(ThresholdSeverity.WARNING);
    }

    @Test
    @DisplayName("HIKARICP_POOL_USAGE·THREADPOOL_QUEUE_USAGE가 정상 범위면 이벤트를 발행하지 않는다")
    void evaluate_poolsNormal_doesNotPublishEvent() {
        // given
        Long serverId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        ServerThreshold hikariThreshold = ServerThreshold.builder()
                .metricKey(ServerMetricKey.HIKARICP_POOL_USAGE)
                .warningValue(80.0)
                .criticalValue(90.0)
                .build();
        ServerThreshold queueThreshold = ServerThreshold.builder()
                .metricKey(ServerMetricKey.THREADPOOL_QUEUE_USAGE)
                .warningValue(50.0)
                .criticalValue(80.0)
                .build();
        ServerHikariCpPoolMetric pool = ServerHikariCpPoolMetric.builder()
                .active(2)
                .max(10) // 사용률 20%
                .build();
        ServerExecutorMetric executor = ServerExecutorMetric.builder()
                .queuedTasks(1)
                .queueRemaining(9) // 사용률 10%
                .build();
        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetric.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .jvmMetric(JvmMetric.builder().build())
                .hikaricpPools(List.of(pool))
                .executors(List.of(executor))
                .build();

        given(serverThresholdRepository.findByServerId(serverId))
                .willReturn(List.of(hikariThreshold, queueThreshold));

        // when
        serverThresholdEvaluationService.evaluate(serverId, collectedAt, realtimeMetric, null);

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("해당 지표의 임계치가 없으면 이벤트를 발행하지 않는다")
    void evaluate_noThresholdConfigured_doesNotPublishEvent() {
        // given
        Long serverId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(999_000_000L)
                .jvmHeapMaxBytes(1_000_000_000L)
                .build();
        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetric.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .jvmMetric(jvmMetric)
                .build();

        given(serverThresholdRepository.findByServerId(serverId)).willReturn(Collections.emptyList());

        // when
        serverThresholdEvaluationService.evaluate(serverId, collectedAt, realtimeMetric, null);

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }
}
