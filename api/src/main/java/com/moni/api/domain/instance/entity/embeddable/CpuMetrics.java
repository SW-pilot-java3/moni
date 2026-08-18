package com.moni.api.domain.instance.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CpuMetrics {

    @Column(name = "cpu_seconds_total")
    private Double cpuSecondsTotal;

    @Column(name = "cpu_idle_seconds_total")
    private Double cpuIdleSecondsTotal;

    @Column(name = "cpu_iowait_seconds_total")
    private Double cpuIowaitSecondsTotal;

    @Builder
    public CpuMetrics(Double cpuSecondsTotal, Double cpuIdleSecondsTotal, Double cpuIowaitSecondsTotal) {
        this.cpuSecondsTotal = cpuSecondsTotal;
        this.cpuIdleSecondsTotal = cpuIdleSecondsTotal;
        this.cpuIowaitSecondsTotal = cpuIowaitSecondsTotal;
    }
}