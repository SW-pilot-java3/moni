package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
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
class InstanceThresholdEvaluationServiceTest {

    @InjectMocks
    private InstanceThresholdEvaluationService instanceThresholdEvaluationService;

    @Mock
    private InstanceThresholdRepository instanceThresholdRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("CPU_USAGE가 critical을 넘으면 CRITICAL 이벤트를 발행한다")
    void evaluate_cpuUsageAboveCritical_publishesCriticalEvent() {
        // given
        Long instanceId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        InstanceThreshold cpuThreshold = InstanceThreshold.builder()
                .metricKey(MetricKey.CPU_USAGE)
                .warningVal(80.0)
                .criticalVal(95.0)
                .build();

        given(instanceThresholdRepository.findAllByInstanceId(instanceId)).willReturn(List.of(cpuThreshold));

        // when
        instanceThresholdEvaluationService.evaluate(instanceId, collectedAt, 98.0, null, Collections.emptyList());

        // then
        ArgumentCaptor<InstanceMetricThresholdExceededEvent> captor =
                ArgumentCaptor.forClass(InstanceMetricThresholdExceededEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        InstanceMetricThresholdExceededEvent event = captor.getValue();
        assertThat(event.instanceId()).isEqualTo(instanceId);
        assertThat(event.metricKey()).isEqualTo(MetricKey.CPU_USAGE);
        assertThat(event.value()).isEqualTo(98.0);
        assertThat(event.severity()).isEqualTo(ThresholdSeverity.CRITICAL);
    }

    @Test
    @DisplayName("MEM_USAGE가 warning 이상 critical 미만이면 WARNING 이벤트를 발행한다")
    void evaluate_memUsageWarningRange_publishesWarningEvent() {
        // given
        Long instanceId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        InstanceThreshold memThreshold = InstanceThreshold.builder()
                .metricKey(MetricKey.MEM_USAGE)
                .warningVal(80.0)
                .criticalVal(95.0)
                .build();
        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memTotalBytes(1_000_000_000L)
                .memAvailableBytes(150_000_000L) // 사용률 85%
                .build();

        given(instanceThresholdRepository.findAllByInstanceId(instanceId)).willReturn(List.of(memThreshold));

        // when
        instanceThresholdEvaluationService.evaluate(instanceId, collectedAt, null, memoryMetrics, Collections.emptyList());

        // then
        ArgumentCaptor<InstanceMetricThresholdExceededEvent> captor =
                ArgumentCaptor.forClass(InstanceMetricThresholdExceededEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().metricKey()).isEqualTo(MetricKey.MEM_USAGE);
        assertThat(captor.getValue().severity()).isEqualTo(ThresholdSeverity.WARNING);
    }

    @Test
    @DisplayName("DISK_USAGE가 정상 범위면 이벤트를 발행하지 않는다")
    void evaluate_diskUsageNormal_doesNotPublishEvent() {
        // given
        Long instanceId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        InstanceThreshold diskThreshold = InstanceThreshold.builder()
                .metricKey(MetricKey.DISK_USAGE)
                .warningVal(80.0)
                .criticalVal(95.0)
                .build();
        InstanceFileSystemMetric filesystem = InstanceFileSystemMetric.builder()
                .mountPoint("/")
                .fsSizeBytes(1_000_000_000L)
                .fsAvailBytes(500_000_000L) // 사용률 50%
                .build();

        given(instanceThresholdRepository.findAllByInstanceId(instanceId)).willReturn(List.of(diskThreshold));

        // when
        instanceThresholdEvaluationService.evaluate(instanceId, collectedAt, null, null, List.of(filesystem));

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("해당 지표의 임계치가 없으면 이벤트를 발행하지 않는다")
    void evaluate_noThresholdConfigured_doesNotPublishEvent() {
        // given
        Long instanceId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();

        given(instanceThresholdRepository.findAllByInstanceId(instanceId)).willReturn(Collections.emptyList());

        // when
        instanceThresholdEvaluationService.evaluate(instanceId, collectedAt, 99.0, null, Collections.emptyList());

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }
}
