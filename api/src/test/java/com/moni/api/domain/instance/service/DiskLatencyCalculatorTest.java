package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiskLatencyCalculatorTest {

    @Test
    @DisplayName("device_name으로 매칭해 여러 장치 중 지연시간(ms)이 가장 높은 값(MAX)을 반환한다")
    void calculate_returnsMaxLatencyAcrossDevices() {
        InstanceDiskMetric prevLow = InstanceDiskMetric.builder()
                .deviceName("sda")
                .readsTotal(100L).writesTotal(100L)
                .ioTimeSecondsTotal(1.0)
                .build();
        InstanceDiskMetric currLow = InstanceDiskMetric.builder()
                .deviceName("sda")
                .readsTotal(200L).writesTotal(200L) // opsDelta = 200
                .ioTimeSecondsTotal(1.2) // ioTimeDelta = 0.2 -> 1ms
                .build();
        InstanceDiskMetric prevHigh = InstanceDiskMetric.builder()
                .deviceName("nvme0n1")
                .readsTotal(0L).writesTotal(0L)
                .ioTimeSecondsTotal(0.0)
                .build();
        InstanceDiskMetric currHigh = InstanceDiskMetric.builder()
                .deviceName("nvme0n1")
                .readsTotal(10L).writesTotal(0L) // opsDelta = 10
                .ioTimeSecondsTotal(1.0) // ioTimeDelta = 1.0 -> 100ms
                .build();

        Double result = DiskLatencyCalculator.calculate(
                List.of(prevLow, prevHigh), List.of(currLow, currHigh));

        assertThat(result).isCloseTo(100.0, within(0.001));
    }

    @Test
    @DisplayName("가상 장치(loop/dm-/ram)는 판정에서 제외한다")
    void calculate_excludesVirtualDevices() {
        InstanceDiskMetric prevLoop = InstanceDiskMetric.builder()
                .deviceName("loop0")
                .readsTotal(0L).writesTotal(0L).ioTimeSecondsTotal(0.0)
                .build();
        InstanceDiskMetric currLoop = InstanceDiskMetric.builder()
                .deviceName("loop0")
                .readsTotal(1L).writesTotal(0L).ioTimeSecondsTotal(10.0) // 극단적으로 높은 값이어도 제외돼야 함
                .build();

        assertThat(DiskLatencyCalculator.calculate(List.of(prevLoop), List.of(currLoop))).isNull();
    }

    @Test
    @DisplayName("이전 레코드에 없던 신규 장치는 이번 윈도우 판정에서 스킵한다")
    void calculate_skipsNewlyAppearedDevice() {
        InstanceDiskMetric currNew = InstanceDiskMetric.builder()
                .deviceName("sdb")
                .readsTotal(10L).writesTotal(10L).ioTimeSecondsTotal(1.0)
                .build();

        assertThat(DiskLatencyCalculator.calculate(List.of(), List.of(currNew))).isNull();
    }

    @Test
    @DisplayName("opsDelta가 0 이하이면(카운터 리셋) 해당 장치는 제외한다")
    void calculate_nonPositiveOpsDelta_excludesDevice() {
        InstanceDiskMetric prev = InstanceDiskMetric.builder()
                .deviceName("sda")
                .readsTotal(100L).writesTotal(100L).ioTimeSecondsTotal(1.0)
                .build();
        InstanceDiskMetric curr = InstanceDiskMetric.builder()
                .deviceName("sda")
                .readsTotal(50L).writesTotal(50L) // 리셋되어 이전보다 작아짐
                .ioTimeSecondsTotal(1.5)
                .build();

        assertThat(DiskLatencyCalculator.calculate(List.of(prev), List.of(curr))).isNull();
    }

    @Test
    @DisplayName("previous 또는 current 리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(DiskLatencyCalculator.calculate(null, List.of())).isNull();
        assertThat(DiskLatencyCalculator.calculate(List.of(), null)).isNull();
    }
}
