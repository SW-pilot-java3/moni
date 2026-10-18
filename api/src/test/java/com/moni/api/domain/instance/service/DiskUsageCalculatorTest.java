package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiskUsageCalculatorTest {

    @Test
    @DisplayName("여러 마운트 중 사용률이 가장 높은 값(MAX)을 반환한다")
    void calculate_returnsMaxUsageAcrossMounts() {
        InstanceFileSystemMetric low = InstanceFileSystemMetric.builder()
                .fsSizeBytes(1_000_000_000L)
                .fsAvailBytes(900_000_000L) // 10% 사용
                .build();
        InstanceFileSystemMetric high = InstanceFileSystemMetric.builder()
                .fsSizeBytes(1_000_000_000L)
                .fsAvailBytes(50_000_000L) // 95% 사용
                .build();

        Double result = DiskUsageCalculator.calculate(List.of(low, high));

        assertThat(result).isCloseTo(95.0, within(0.001));
    }

    @Test
    @DisplayName("fsSizeBytes가 0인 마운트는 건너뛰고 나머지로 계산한다")
    void calculate_skipsZeroSizeMount() {
        InstanceFileSystemMetric invalid = InstanceFileSystemMetric.builder()
                .fsSizeBytes(0L)
                .fsAvailBytes(0L)
                .build();
        InstanceFileSystemMetric valid = InstanceFileSystemMetric.builder()
                .fsSizeBytes(1_000_000_000L)
                .fsAvailBytes(200_000_000L) // 80% 사용
                .build();

        Double result = DiskUsageCalculator.calculate(List.of(invalid, valid));

        assertThat(result).isCloseTo(80.0, within(0.001));
    }

    @Test
    @DisplayName("유효한 마운트가 하나도 없으면 null을 반환한다")
    void calculate_allInvalid_returnsNull() {
        InstanceFileSystemMetric invalid = InstanceFileSystemMetric.builder()
                .fsSizeBytes(null)
                .fsAvailBytes(null)
                .build();

        assertThat(DiskUsageCalculator.calculate(List.of(invalid))).isNull();
    }

    @Test
    @DisplayName("빈 리스트면 null을 반환한다")
    void calculate_emptyList_returnsNull() {
        assertThat(DiskUsageCalculator.calculate(Collections.emptyList())).isNull();
    }

    @Test
    @DisplayName("리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(DiskUsageCalculator.calculate(null)).isNull();
    }
}
