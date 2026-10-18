package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NetErrorRateCalculatorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 24, 0, 0, 0);
    private static final LocalDateTime T10 = T0.plusSeconds(10);

    @Test
    @DisplayName("interface_name으로 매칭해 여러 인터페이스 중 초당 에러 건수가 가장 높은 값(MAX)을 반환한다")
    void calculate_returnsMaxErrorsPerSecAcrossInterfaces() {
        InstanceNetworkMetric prevEth0 = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T0)
                .rxErrorsTotal(0L).txErrorsTotal(0L)
                .build();
        InstanceNetworkMetric currEth0 = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T10)
                .rxErrorsTotal(10L).txErrorsTotal(0L) // 10 errors / 10s = 1.0/s
                .build();
        InstanceNetworkMetric prevEth1 = InstanceNetworkMetric.builder()
                .interfaceName("eth1").collectedAt(T0)
                .rxErrorsTotal(0L).txErrorsTotal(0L)
                .build();
        InstanceNetworkMetric currEth1 = InstanceNetworkMetric.builder()
                .interfaceName("eth1").collectedAt(T10)
                .rxErrorsTotal(50L).txErrorsTotal(0L) // 50 errors / 10s = 5.0/s
                .build();

        Double result = NetErrorRateCalculator.calculate(
                List.of(prevEth0, prevEth1), List.of(currEth0, currEth1));

        assertThat(result).isCloseTo(5.0, within(0.001));
    }

    @Test
    @DisplayName("이전 레코드에 없던 신규 인터페이스는 이번 윈도우 판정에서 스킵한다")
    void calculate_skipsNewlyAppearedInterface() {
        InstanceNetworkMetric currNew = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T10)
                .rxErrorsTotal(10L).txErrorsTotal(0L)
                .build();

        assertThat(NetErrorRateCalculator.calculate(List.of(), List.of(currNew))).isNull();
    }

    @Test
    @DisplayName("경과 시간이 0 이하이면 null을 반환한다")
    void calculate_nonPositiveElapsedSeconds_returnsNull() {
        InstanceNetworkMetric prev = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T10)
                .rxErrorsTotal(0L).txErrorsTotal(0L)
                .build();
        InstanceNetworkMetric curr = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T0) // 이전보다 과거
                .rxErrorsTotal(10L).txErrorsTotal(0L)
                .build();

        assertThat(NetErrorRateCalculator.calculate(List.of(prev), List.of(curr))).isNull();
    }

    @Test
    @DisplayName("에러 카운터가 리셋되어 델타가 음수이면 null을 반환한다")
    void calculate_negativeErrorDelta_returnsNull() {
        InstanceNetworkMetric prev = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T0)
                .rxErrorsTotal(100L).txErrorsTotal(0L)
                .build();
        InstanceNetworkMetric curr = InstanceNetworkMetric.builder()
                .interfaceName("eth0").collectedAt(T10)
                .rxErrorsTotal(10L).txErrorsTotal(0L) // 리셋되어 이전보다 작아짐
                .build();

        assertThat(NetErrorRateCalculator.calculate(List.of(prev), List.of(curr))).isNull();
    }

    @Test
    @DisplayName("previous 또는 current 리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(NetErrorRateCalculator.calculate(null, List.of())).isNull();
        assertThat(NetErrorRateCalculator.calculate(List.of(), null)).isNull();
    }
}
