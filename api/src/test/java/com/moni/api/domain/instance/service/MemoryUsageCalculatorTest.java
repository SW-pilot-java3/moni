package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoryUsageCalculatorTest {

    @Test
    @DisplayName("memTotalBytes 대비 memAvailableBytes 비율로 사용률(%)을 계산한다")
    void calculate_returnsUsagePercent() {
        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memTotalBytes(8_000_000_000L)
                .memAvailableBytes(2_000_000_000L)
                .build();

        Double result = MemoryUsageCalculator.calculate(memoryMetrics);

        assertThat(result).isCloseTo(75.0, within(0.001));
    }

    @Test
    @DisplayName("memoryMetrics가 null이면 null을 반환한다")
    void calculate_nullMemoryMetrics_returnsNull() {
        assertThat(MemoryUsageCalculator.calculate(null)).isNull();
    }

    @Test
    @DisplayName("memTotalBytes가 null이면 null을 반환한다")
    void calculate_nullTotal_returnsNull() {
        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memAvailableBytes(2_000_000_000L)
                .build();

        assertThat(MemoryUsageCalculator.calculate(memoryMetrics)).isNull();
    }

    @Test
    @DisplayName("memTotalBytes가 0이면 null을 반환한다")
    void calculate_zeroTotal_returnsNull() {
        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memTotalBytes(0L)
                .memAvailableBytes(0L)
                .build();

        assertThat(MemoryUsageCalculator.calculate(memoryMetrics)).isNull();
    }

    @Test
    @DisplayName("memAvailableBytes가 null이면 null을 반환한다")
    void calculate_nullAvailable_returnsNull() {
        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memTotalBytes(8_000_000_000L)
                .build();

        assertThat(MemoryUsageCalculator.calculate(memoryMetrics)).isNull();
    }
}
