package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.JvmMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GcPauseTimeCalculatorTest {

    @Test
    @DisplayName("gc_pause_seconds_sum/count 델타 비율로 평균 1회당 정지시간(초)을 계산한다")
    void calculate_returnsAvgPauseSeconds() {
        JvmMetric previous = JvmMetric.builder()
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(1.0)
                .build();
        JvmMetric current = JvmMetric.builder()
                .gcPauseSecondsCount(14L) // countDelta = 4
                .gcPauseSecondsSum(1.8) // sumDelta = 0.8 -> 평균 0.2초
                .build();

        Double result = GcPauseTimeCalculator.calculate(previous, current);

        assertThat(result).isCloseTo(0.2, within(0.0001));
    }

    @Test
    @DisplayName("previous가 null이면 null을 반환한다")
    void calculate_nullPrevious_returnsNull() {
        JvmMetric current = JvmMetric.builder()
                .gcPauseSecondsCount(14L)
                .gcPauseSecondsSum(1.8)
                .build();

        assertThat(GcPauseTimeCalculator.calculate(null, current)).isNull();
    }

    @Test
    @DisplayName("current가 null이면 null을 반환한다")
    void calculate_nullCurrent_returnsNull() {
        JvmMetric previous = JvmMetric.builder()
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(1.0)
                .build();

        assertThat(GcPauseTimeCalculator.calculate(previous, null)).isNull();
    }

    @Test
    @DisplayName("countDelta가 0 이하이면(GC 미발생 또는 카운터 리셋) null을 반환한다")
    void calculate_nonPositiveCountDelta_returnsNull() {
        JvmMetric previous = JvmMetric.builder()
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(1.0)
                .build();
        JvmMetric current = JvmMetric.builder()
                .gcPauseSecondsCount(10L) // countDelta = 0
                .gcPauseSecondsSum(1.0)
                .build();

        assertThat(GcPauseTimeCalculator.calculate(previous, current)).isNull();
    }

    @Test
    @DisplayName("sumDelta가 음수이면(카운터 리셋) null을 반환한다")
    void calculate_negativeSumDelta_returnsNull() {
        JvmMetric previous = JvmMetric.builder()
                .gcPauseSecondsCount(10L)
                .gcPauseSecondsSum(2.0)
                .build();
        JvmMetric current = JvmMetric.builder()
                .gcPauseSecondsCount(14L)
                .gcPauseSecondsSum(1.0) // 리셋되어 이전보다 작아짐
                .build();

        assertThat(GcPauseTimeCalculator.calculate(previous, current)).isNull();
    }
}
