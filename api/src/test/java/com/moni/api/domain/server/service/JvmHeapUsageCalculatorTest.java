package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.JvmMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JvmHeapUsageCalculatorTest {

    @Test
    @DisplayName("jvmHeapUsedBytes 대비 jvmHeapMaxBytes 비율로 사용률(%)을 계산한다")
    void calculate_returnsUsagePercent() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(200_000_000L)
                .jvmHeapMaxBytes(400_000_000L)
                .build();

        Double result = JvmHeapUsageCalculator.calculate(jvmMetric);

        assertThat(result).isCloseTo(50.0, within(0.001));
    }

    @Test
    @DisplayName("jvmMetric이 null이면 null을 반환한다")
    void calculate_nullJvmMetric_returnsNull() {
        assertThat(JvmHeapUsageCalculator.calculate(null)).isNull();
    }

    @Test
    @DisplayName("jvmHeapMaxBytes가 null이면 null을 반환한다")
    void calculate_nullMax_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(200_000_000L)
                .build();

        assertThat(JvmHeapUsageCalculator.calculate(jvmMetric)).isNull();
    }

    @Test
    @DisplayName("jvmHeapMaxBytes가 0 이하이면 null을 반환한다")
    void calculate_nonPositiveMax_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(200_000_000L)
                .jvmHeapMaxBytes(0L)
                .build();

        assertThat(JvmHeapUsageCalculator.calculate(jvmMetric)).isNull();
    }

    @Test
    @DisplayName("jvmHeapUsedBytes가 null이면 null을 반환한다")
    void calculate_nullUsed_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapMaxBytes(400_000_000L)
                .build();

        assertThat(JvmHeapUsageCalculator.calculate(jvmMetric)).isNull();
    }
}
