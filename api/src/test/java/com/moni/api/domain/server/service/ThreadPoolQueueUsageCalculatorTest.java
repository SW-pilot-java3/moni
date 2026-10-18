package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.ServerExecutorMetric;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThreadPoolQueueUsageCalculatorTest {

    @Test
    @DisplayName("큐 용량이 유한하면 queuedTasks/capacity 비율(%)로 계산한다")
    void calculate_boundedQueue_returnsUsagePercent() {
        ServerExecutorMetric executor = ServerExecutorMetric.builder()
                .queuedTasks(10)
                .queueRemaining(40) // capacity = 50, 사용률 20%
                .build();

        Double result = ThreadPoolQueueUsageCalculator.calculate(List.of(executor));

        assertThat(result).isCloseTo(20.0, within(0.001));
    }

    @Test
    @DisplayName("queueRemaining이 100만 이상이면 무제한 큐로 보고 판정에서 제외한다")
    void calculate_unboundedQueue_returnsNull() {
        ServerExecutorMetric defaultExecutor = ServerExecutorMetric.builder()
                .queuedTasks(5)
                .queueRemaining(Integer.MAX_VALUE - 5) // 설정 안 한 기본값에 가까운 상태
                .build();

        assertThat(ThreadPoolQueueUsageCalculator.calculate(List.of(defaultExecutor))).isNull();
    }

    @Test
    @DisplayName("유한한 큐와 무제한 큐가 섞여 있으면 유한한 큐만으로 MAX를 계산한다")
    void calculate_mixedExecutors_ignoresUnboundedOnly() {
        ServerExecutorMetric unbounded = ServerExecutorMetric.builder()
                .queuedTasks(100)
                .queueRemaining(2_000_000)
                .build();
        ServerExecutorMetric bounded = ServerExecutorMetric.builder()
                .queuedTasks(9)
                .queueRemaining(1) // capacity = 10, 사용률 90%
                .build();

        Double result = ThreadPoolQueueUsageCalculator.calculate(List.of(unbounded, bounded));

        assertThat(result).isCloseTo(90.0, within(0.001));
    }

    @Test
    @DisplayName("null 게이지(NaN으로 수집 안 된 executor)는 0으로 취급하지 않고 건너뛴다")
    void calculate_nullGauge_skips() {
        ServerExecutorMetric nullGauge = ServerExecutorMetric.builder()
                .queuedTasks(null)
                .queueRemaining(null)
                .build();

        assertThat(ThreadPoolQueueUsageCalculator.calculate(List.of(nullGauge))).isNull();
    }

    @Test
    @DisplayName("빈 리스트면 null을 반환한다")
    void calculate_emptyList_returnsNull() {
        assertThat(ThreadPoolQueueUsageCalculator.calculate(Collections.emptyList())).isNull();
    }

    @Test
    @DisplayName("리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(ThreadPoolQueueUsageCalculator.calculate(null)).isNull();
    }
}
