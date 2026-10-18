package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HikariPoolUsageCalculatorTest {

    @Test
    @DisplayName("여러 풀 중 사용률이 가장 높은 값(MAX)을 반환한다")
    void calculate_returnsMaxUsageAcrossPools() {
        ServerHikariCpPoolMetric low = ServerHikariCpPoolMetric.builder()
                .active(2)
                .max(10) // 20% 사용
                .build();
        ServerHikariCpPoolMetric high = ServerHikariCpPoolMetric.builder()
                .active(9)
                .max(10) // 90% 사용
                .build();

        Double result = HikariPoolUsageCalculator.calculate(List.of(low, high));

        assertThat(result).isCloseTo(90.0, within(0.001));
    }

    @Test
    @DisplayName("max가 0 이하인 풀은 건너뛰고 나머지로 계산한다")
    void calculate_skipsNonPositiveMaxPool() {
        ServerHikariCpPoolMetric invalid = ServerHikariCpPoolMetric.builder()
                .active(0)
                .max(0)
                .build();
        ServerHikariCpPoolMetric valid = ServerHikariCpPoolMetric.builder()
                .active(8)
                .max(10) // 80% 사용
                .build();

        Double result = HikariPoolUsageCalculator.calculate(List.of(invalid, valid));

        assertThat(result).isCloseTo(80.0, within(0.001));
    }

    @Test
    @DisplayName("null 게이지(active/max)를 가진 풀은 0으로 취급하지 않고 건너뛴다")
    void calculate_skipsNullGaugePool() {
        ServerHikariCpPoolMetric nullGauge = ServerHikariCpPoolMetric.builder()
                .active(null)
                .max(null)
                .build();

        assertThat(HikariPoolUsageCalculator.calculate(List.of(nullGauge))).isNull();
    }

    @Test
    @DisplayName("빈 리스트면 null을 반환한다")
    void calculate_emptyList_returnsNull() {
        assertThat(HikariPoolUsageCalculator.calculate(Collections.emptyList())).isNull();
    }

    @Test
    @DisplayName("리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(HikariPoolUsageCalculator.calculate(null)).isNull();
    }
}
