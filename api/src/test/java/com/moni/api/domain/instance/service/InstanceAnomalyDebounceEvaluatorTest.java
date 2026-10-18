package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;
import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.repository.InstanceDiskMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceFileSystemMetricRepository;
import com.moni.api.domain.instance.repository.InstanceNetworkMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InstanceAnomalyDebounceEvaluatorTest {

    private InstanceAnomalyDebounceEvaluator evaluator;

    @Mock
    private InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;

    @Mock
    private InstanceFileSystemMetricRepository instanceFileSystemMetricRepository;

    @Mock
    private InstanceDiskMetricsRepository instanceDiskMetricsRepository;

    @Mock
    private InstanceNetworkMetricsRepository instanceNetworkMetricsRepository;

    @BeforeEach
    void setUp() {
        evaluator = new InstanceAnomalyDebounceEvaluator(instanceRealtimeMetricRepository,
                instanceFileSystemMetricRepository, instanceDiskMetricsRepository, instanceNetworkMetricsRepository);
    }

    private final InstanceThreshold cpuThreshold = InstanceThreshold.builder()
            .metricKey(MetricKey.CPU_USAGE).warningVal(80.0).criticalVal(95.0).build();
    private final InstanceThreshold memThreshold = InstanceThreshold.builder()
            .metricKey(MetricKey.MEM_USAGE).warningVal(80.0).criticalVal(95.0).build();
    private final InstanceThreshold diskLatencyThreshold = InstanceThreshold.builder()
            .metricKey(MetricKey.DISK_LATENCY).warningVal(100.0).criticalVal(500.0).build();
    private final InstanceThreshold netErrorRateThreshold = InstanceThreshold.builder()
            .metricKey(MetricKey.NET_ERROR_RATE).warningVal(1.0).criticalVal(5.0).build();

    @Test
    @DisplayName("CPU_USAGE 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_cpuUsageThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: totalDelta=10, idleDelta=1 -> 매 구간 사용률 90%
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                metricRow(now, cpuMetrics(130.0, 53.0)),
                metricRow(now.minusSeconds(10), cpuMetrics(120.0, 52.0)),
                metricRow(now.minusSeconds(20), cpuMetrics(110.0, 51.0)),
                metricRow(now.minusSeconds(30), cpuMetrics(100.0, 50.0))
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.CPU_USAGE, cpuThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("MEM_USAGE 게이지 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_memUsageThreeConsecutiveGaugesExceed_returnsTrue() {
        // given: 사용률 90% (available 100 / total 1000)
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                memoryRow(now, 1000L, 100L),
                memoryRow(now.minusSeconds(10), 1000L, 100L),
                memoryRow(now.minusSeconds(20), 1000L, 100L)
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.MEM_USAGE, memThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유효값이 3개 미만이면 false를 반환한다 (fail-closed)")
    void isConsecutivelyExceeded_fewerThanThreeValidValues_returnsFalse() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                memoryRow(now, 1000L, 100L),
                memoryRow(now.minusSeconds(10), 1000L, 100L)
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.MEM_USAGE, memThreshold);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("최근 3개 중 하나라도 임계치 미만이면 false를 반환한다")
    void isConsecutivelyExceeded_oneOfThreeBelowThreshold_returnsFalse() {
        // given: 마지막 한 건만 사용률 50%로 정상 범위
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                memoryRow(now, 1000L, 100L),
                memoryRow(now.minusSeconds(10), 1000L, 100L),
                memoryRow(now.minusSeconds(20), 1000L, 500L)
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.MEM_USAGE, memThreshold);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("DISK_LATENCY 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_diskLatencyThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: opsDelta=10, ioTimeDelta=6.0 -> 매 구간 지연시간 600ms
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                metricRow(now, null),
                metricRow(now.minusSeconds(10), null),
                metricRow(now.minusSeconds(20), null),
                metricRow(now.minusSeconds(30), null)
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        List<InstanceDiskMetric> diskRows = List.of(
                diskMetric(now, 130L, 19.0),
                diskMetric(now.minusSeconds(10), 120L, 13.0),
                diskMetric(now.minusSeconds(20), 110L, 7.0),
                diskMetric(now.minusSeconds(30), 100L, 1.0)
        );
        given(instanceDiskMetricsRepository.findAllByInstanceIdAndCollectedAtBetween(eq(1L), any(), any()))
                .willReturn(diskRows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.DISK_LATENCY, diskLatencyThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("NET_ERROR_RATE 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_netErrorRateThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: 10초당 rxErrors 100건 증가 -> 초당 10건 (critical 5.0 초과)
        LocalDateTime now = LocalDateTime.now();
        List<InstanceRealtimeMetric> rows = List.of(
                metricRow(now, null),
                metricRow(now.minusSeconds(10), null),
                metricRow(now.minusSeconds(20), null),
                metricRow(now.minusSeconds(30), null)
        );
        given(instanceRealtimeMetricRepository.findAllByInstanceIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        List<InstanceNetworkMetric> networkRows = List.of(
                networkMetric(now, 300L),
                networkMetric(now.minusSeconds(10), 200L),
                networkMetric(now.minusSeconds(20), 100L),
                networkMetric(now.minusSeconds(30), 0L)
        );
        given(instanceNetworkMetricsRepository.findAllByInstanceIdAndCollectedAtBetween(eq(1L), any(), any()))
                .willReturn(networkRows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, MetricKey.NET_ERROR_RATE, netErrorRateThreshold);

        // then
        assertThat(result).isTrue();
    }

    private InstanceDiskMetric diskMetric(LocalDateTime collectedAt, Long readsTotal, Double ioTimeSecondsTotal) {
        return InstanceDiskMetric.builder()
                .collectedAt(collectedAt)
                .deviceName("sda")
                .readsTotal(readsTotal)
                .writesTotal(0L)
                .ioTimeSecondsTotal(ioTimeSecondsTotal)
                .build();
    }

    private InstanceNetworkMetric networkMetric(LocalDateTime collectedAt, Long rxErrorsTotal) {
        return InstanceNetworkMetric.builder()
                .collectedAt(collectedAt)
                .interfaceName("eth0")
                .rxErrorsTotal(rxErrorsTotal)
                .txErrorsTotal(0L)
                .build();
    }

    private InstanceRealtimeMetric metricRow(LocalDateTime collectedAt, CpuMetrics cpuMetrics) {
        return InstanceRealtimeMetric.builder()
                .collectedAt(collectedAt)
                .cpuMetrics(cpuMetrics)
                .build();
    }

    private InstanceRealtimeMetric memoryRow(LocalDateTime collectedAt, Long total, Long available) {
        return InstanceRealtimeMetric.builder()
                .collectedAt(collectedAt)
                .memoryMetrics(MemoryMetrics.builder().memTotalBytes(total).memAvailableBytes(available).build())
                .build();
    }

    private CpuMetrics cpuMetrics(Double total, Double idle) {
        return CpuMetrics.builder().cpuSecondsTotal(total).cpuIdleSecondsTotal(idle).build();
    }
}
