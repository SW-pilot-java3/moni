package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.JvmMetric;

final class JvmHeapUsageCalculator {

    private JvmHeapUsageCalculator() {
    }

    static Double calculate(JvmMetric jvmMetric) {
        if (jvmMetric == null) {
            return null;
        }

        Long heapUsedBytes = jvmMetric.getJvmHeapUsedBytes();
        Long heapMaxBytes = jvmMetric.getJvmHeapMaxBytes();

        if (heapUsedBytes == null || heapMaxBytes == null || heapMaxBytes <= 0) {
            return null;
        }

        return ((double) heapUsedBytes / heapMaxBytes) * 100;
    }
}
