package com.moni.api.domain.report.daily.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HourlyStatAggregatorTest {

    @Test
    @DisplayName("avgOfAvg는 평균들의 평균을 낸다 (단순 합산이 아님)")
    void avgOfAvg_returnsMeanOfMeans() {
        List<Double> hourlyCpuAverages = List.of(70.0, 65.0, 80.0, 75.0);

        Double result = HourlyStatAggregator.avgOfAvg(hourlyCpuAverages);

        assertThat(result).isCloseTo(72.5, within(0.001));
        assertThat(result).isLessThanOrEqualTo(100.0); // 단순 합산이었다면 290이 나와야 함
    }

    @Test
    @DisplayName("avgOfAvg는 null 값을 제외하고 계산한다")
    void avgOfAvg_ignoresNulls() {
        List<Double> values = Arrays.asList(80.0, null, 60.0);

        assertThat(HourlyStatAggregator.avgOfAvg(values)).isCloseTo(70.0, within(0.001));
    }

    @Test
    @DisplayName("모든 값이 null이면 avgOfAvg는 null을 반환한다")
    void avgOfAvg_allNull_returnsNull() {
        assertThat(HourlyStatAggregator.avgOfAvg(Arrays.asList((Double) null, null))).isNull();
    }

    @Test
    @DisplayName("maxOfMax는 여러 5분 최댓값 중 가장 큰 값을 반환한다")
    void maxOfMax_returnsOverallMax() {
        List<Double> hourlyCpuMaxes = List.of(90.0, 95.0, 88.0);

        assertThat(HourlyStatAggregator.maxOfMax(hourlyCpuMaxes)).isCloseTo(95.0, within(0.001));
    }

    @Test
    @DisplayName("minOfMin은 여러 5분 최솟값 중 가장 작은 값을 반환한다")
    void minOfMin_returnsOverallMin() {
        List<Long> hourlyMemMins = List.of(2_000_000_000L, 1_500_000_000L, 1_800_000_000L);

        assertThat(HourlyStatAggregator.minOfMin(hourlyMemMins)).isEqualTo(1_500_000_000L);
    }

    @Test
    @DisplayName("sum은 여러 5분 발생 건수를 그대로 더한다 (유일하게 합산이 맞는 케이스)")
    void sum_addsCounts() {
        List<Integer> hourlyErrorCounts = List.of(1, 0, 2, 0);

        assertThat(HourlyStatAggregator.sum(hourlyErrorCounts)).isEqualTo(3);
    }

    @Test
    @DisplayName("avgOfAvgLong은 Long 평균값들의 평균을 반올림해 반환한다")
    void avgOfAvgLong_returnsRoundedMean() {
        List<Long> values = List.of(1_000L, 2_000L, 3_000L);

        assertThat(HourlyStatAggregator.avgOfAvgLong(values)).isEqualTo(2_000L);
    }
}
