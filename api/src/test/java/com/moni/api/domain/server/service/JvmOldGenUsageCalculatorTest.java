package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.JvmMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JvmOldGenUsageCalculatorTest {

    @Test
    @DisplayName("jvmOldGenUsedBytes 대비 jvmOldGenMaxBytes 비율로 사용률(%)을 계산한다")
    void calculate_returnsUsagePercent() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmOldGenUsedBytes(150_000_000L)
                .jvmOldGenMaxBytes(300_000_000L)
                .build();

        Double result = JvmOldGenUsageCalculator.calculate(jvmMetric);

        assertThat(result).isCloseTo(50.0, within(0.001));
    }

    @Test
    @DisplayName("jvmMetric이 null이면 null을 반환한다")
    void calculate_nullJvmMetric_returnsNull() {
        assertThat(JvmOldGenUsageCalculator.calculate(null)).isNull();
    }

    @Test
    @DisplayName("jvmOldGenMaxBytes가 null이면 null을 반환한다")
    void calculate_nullMax_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmOldGenUsedBytes(150_000_000L)
                .build();

        assertThat(JvmOldGenUsageCalculator.calculate(jvmMetric)).isNull();
    }

    @Test
    @DisplayName("jvmOldGenMaxBytes가 0 이하이면 null을 반환한다")
    void calculate_nonPositiveMax_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmOldGenUsedBytes(150_000_000L)
                .jvmOldGenMaxBytes(0L)
                .build();

        assertThat(JvmOldGenUsageCalculator.calculate(jvmMetric)).isNull();
    }

    @Test
    @DisplayName("jvmOldGenUsedBytes가 null이면 null을 반환한다")
    void calculate_nullUsed_returnsNull() {
        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmOldGenMaxBytes(300_000_000L)
                .build();

        assertThat(JvmOldGenUsageCalculator.calculate(jvmMetric)).isNull();
    }
}
