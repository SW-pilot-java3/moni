package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CpuUsageCalculatorTest {

    @Test
    @DisplayName("idle/total 델타 비율로 CPU 사용률(%)을 계산한다")
    void calculate_returnsUsagePercent() {
        CpuMetrics previous = CpuMetrics.builder()
                .cpuSecondsTotal(100.0)
                .cpuIdleSecondsTotal(80.0)
                .build();
        CpuMetrics current = CpuMetrics.builder()
                .cpuSecondsTotal(110.0) // totalDelta = 10
                .cpuIdleSecondsTotal(85.0) // idleDelta = 5 -> 사용률 50%
                .build();

        Double result = CpuUsageCalculator.calculate(previous, current);

        assertThat(result).isCloseTo(50.0, within(0.001));
    }

    @Test
    @DisplayName("previous가 null이면 null을 반환한다")
    void calculate_nullPrevious_returnsNull() {
        CpuMetrics current = CpuMetrics.builder()
                .cpuSecondsTotal(110.0)
                .cpuIdleSecondsTotal(85.0)
                .build();

        assertThat(CpuUsageCalculator.calculate(null, current)).isNull();
    }

    @Test
    @DisplayName("totalDelta가 0 이하이면(카운터 리셋) null을 반환한다")
    void calculate_nonPositiveTotalDelta_returnsNull() {
        CpuMetrics previous = CpuMetrics.builder()
                .cpuSecondsTotal(100.0)
                .cpuIdleSecondsTotal(80.0)
                .build();
        CpuMetrics current = CpuMetrics.builder()
                .cpuSecondsTotal(50.0) // 리셋되어 이전보다 작아짐
                .cpuIdleSecondsTotal(40.0)
                .build();

        assertThat(CpuUsageCalculator.calculate(previous, current)).isNull();
    }

    @Test
    @DisplayName("idleDelta가 음수이면 null을 반환한다")
    void calculate_negativeIdleDelta_returnsNull() {
        CpuMetrics previous = CpuMetrics.builder()
                .cpuSecondsTotal(100.0)
                .cpuIdleSecondsTotal(80.0)
                .build();
        CpuMetrics current = CpuMetrics.builder()
                .cpuSecondsTotal(110.0) // totalDelta = 10
                .cpuIdleSecondsTotal(70.0) // idleDelta = -10 (비정상)
                .build();

        assertThat(CpuUsageCalculator.calculate(previous, current)).isNull();
    }

    @Test
    @DisplayName("idleDelta가 totalDelta보다 크면(사용률이 음수가 되는 경우) null을 반환한다")
    void calculate_idleDeltaExceedsTotalDelta_returnsNull() {
        CpuMetrics previous = CpuMetrics.builder()
                .cpuSecondsTotal(100.0)
                .cpuIdleSecondsTotal(80.0)
                .build();
        CpuMetrics current = CpuMetrics.builder()
                .cpuSecondsTotal(110.0) // totalDelta = 10
                .cpuIdleSecondsTotal(95.0) // idleDelta = 15 > totalDelta
                .build();

        assertThat(CpuUsageCalculator.calculate(previous, current)).isNull();
    }
}
