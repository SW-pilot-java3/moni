package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;

final class MemoryUsageCalculator {

    private MemoryUsageCalculator() {
    }

    static Double calculate(MemoryMetrics memoryMetrics) {
        if (memoryMetrics == null) {
            return null;
        }

        Long memTotalBytes = memoryMetrics.getMemTotalBytes();
        Long memAvailableBytes = memoryMetrics.getMemAvailableBytes();

        if (memTotalBytes == null || memTotalBytes <= 0 || memAvailableBytes == null) {
            return null;
        }

        return (1 - ((double) memAvailableBytes / memTotalBytes)) * 100;
    }
}
