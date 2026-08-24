package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
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
class ServerAnomalyDebounceEvaluatorTest {

    private ServerAnomalyDebounceEvaluator evaluator;

    @Mock
    private ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    private final ServerThreshold heapThreshold = ServerThreshold.builder()
            .metricKey(ServerMetricKey.JVM_HEAP_USAGE).warningValue(80.0).criticalValue(90.0).build();
    private final ServerThreshold gcThreshold = ServerThreshold.builder()
            .metricKey(ServerMetricKey.GC_PAUSE_TIME).warningValue(0.5).criticalValue(1.0).build();
    private final ServerThreshold httpAvgLatencyThreshold = ServerThreshold.builder()
            .metricKey(ServerMetricKey.HTTP_AVG_LATENCY).warningValue(500.0).criticalValue(1000.0).build();
    private final ServerThreshold httpErrorRateThreshold = ServerThreshold.builder()
            .metricKey(ServerMetricKey.HTTP_ERROR_RATE).warningValue(1.0).criticalValue(5.0).build();

    @BeforeEach
    void setUp() {
        evaluator = new ServerAnomalyDebounceEvaluator(serverRealtimeMetricRepository);
    }

    @Test
    @DisplayName("JVM_HEAP_USAGE 게이지 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_jvmHeapUsageThreeConsecutiveGaugesExceed_returnsTrue() {
        // given: 사용률 95% (used 950 / max 1000)
        LocalDateTime now = LocalDateTime.now();
        List<ServerRealtimeMetric> rows = List.of(
                heapRow(now, 950L, 1000L),
                heapRow(now.minusSeconds(10), 950L, 1000L),
                heapRow(now.minusSeconds(20), 950L, 1000L)
        );
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, ServerMetricKey.JVM_HEAP_USAGE, heapThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("GC_PAUSE_TIME 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_gcPauseTimeThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: countDelta=2, sumDelta=1.6 -> 평균 0.8초 (warning 0.5 초과)
        LocalDateTime now = LocalDateTime.now();
        List<ServerRealtimeMetric> rows = List.of(
                gcRow(now, 12L, 14.8),
                gcRow(now.minusSeconds(10), 10L, 13.2),
                gcRow(now.minusSeconds(20), 8L, 11.6),
                gcRow(now.minusSeconds(30), 6L, 10.0)
        );
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, ServerMetricKey.GC_PAUSE_TIME, gcThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유효값이 3개 미만이면 false를 반환한다 (fail-closed)")
    void isConsecutivelyExceeded_fewerThanThreeValidValues_returnsFalse() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<ServerRealtimeMetric> rows = List.of(
                heapRow(now, 950L, 1000L),
                heapRow(now.minusSeconds(10), 950L, 1000L)
        );
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, ServerMetricKey.JVM_HEAP_USAGE, heapThreshold);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("HTTP_AVG_LATENCY 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_httpAvgLatencyThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: countDelta=100, sumDelta=150.0(초) -> 매 구간 평균 1500ms
        LocalDateTime now = LocalDateTime.now();
        List<ServerRealtimeMetric> rows = List.of(
                httpRow(now, 400L, 600.0, "200"),
                httpRow(now.minusSeconds(10), 300L, 450.0, "200"),
                httpRow(now.minusSeconds(20), 200L, 300.0, "200"),
                httpRow(now.minusSeconds(30), 100L, 150.0, "200")
        );
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, ServerMetricKey.HTTP_AVG_LATENCY, httpAvgLatencyThreshold);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("HTTP_ERROR_RATE 델타 3개가 연속으로 임계치를 초과하면 true를 반환한다")
    void isConsecutivelyExceeded_httpErrorRateThreeConsecutiveDeltasExceed_returnsTrue() {
        // given: 모든 요청이 5xx -> 매 구간 에러율 100%
        LocalDateTime now = LocalDateTime.now();
        List<ServerRealtimeMetric> rows = List.of(
                httpRow(now, 400L, null, "500"),
                httpRow(now.minusSeconds(10), 300L, null, "500"),
                httpRow(now.minusSeconds(20), 200L, null, "500"),
                httpRow(now.minusSeconds(30), 100L, null, "500")
        );
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(rows);

        // when
        boolean result = evaluator.isConsecutivelyExceeded(1L, ServerMetricKey.HTTP_ERROR_RATE, httpErrorRateThreshold);

        // then
        assertThat(result).isTrue();
    }

    private ServerRealtimeMetric httpRow(LocalDateTime collectedAt, Long requestsCount, Double requestsSum, String status) {
        ServerHttpEndpointMetric endpoint = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status(status)
                .requestsCount(requestsCount).requestsSum(requestsSum)
                .build();
        return ServerRealtimeMetric.builder()
                .collectedAt(collectedAt)
                .httpEndpoints(List.of(endpoint))
                .build();
    }

    private ServerRealtimeMetric heapRow(LocalDateTime collectedAt, Long used, Long max) {
        return ServerRealtimeMetric.builder()
                .collectedAt(collectedAt)
                .jvmMetric(JvmMetric.builder().jvmHeapUsedBytes(used).jvmHeapMaxBytes(max).build())
                .build();
    }

    private ServerRealtimeMetric gcRow(LocalDateTime collectedAt, Long count, Double sum) {
        return ServerRealtimeMetric.builder()
                .collectedAt(collectedAt)
                .jvmMetric(JvmMetric.builder().gcPauseSecondsCount(count).gcPauseSecondsSum(sum).build())
                .build();
    }
}
