package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;

final class CpuUsageCalculator {

    private CpuUsageCalculator() {
    }

    static Double calculate(CpuMetrics previous, CpuMetrics current) {
        if (previous == null || current == null) {
            return null;
        }

        Double prevTotal = previous.getCpuSecondsTotal();
        Double prevIdle = previous.getCpuIdleSecondsTotal();
        Double currTotal = current.getCpuSecondsTotal();
        Double currIdle = current.getCpuIdleSecondsTotal();

        if (prevTotal == null || prevIdle == null || currTotal == null || currIdle == null) {
            return null;
        }

        double totalDelta = currTotal - prevTotal;
        double idleDelta = currIdle - prevIdle;

        if (totalDelta <= 0) {
            return null;
        }

        return (1 - (idleDelta / totalDelta)) * 100;
    }
}