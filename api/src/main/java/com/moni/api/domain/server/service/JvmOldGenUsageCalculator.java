package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.JvmMetric;

final class JvmOldGenUsageCalculator {

    private JvmOldGenUsageCalculator() {
    }

    static Double calculate(JvmMetric jvmMetric) {
        if (jvmMetric == null) {
            return null;
        }

        Long oldGenUsedBytes = jvmMetric.getJvmOldGenUsedBytes();
        Long oldGenMaxBytes = jvmMetric.getJvmOldGenMaxBytes();

        if (oldGenUsedBytes == null || oldGenMaxBytes == null || oldGenMaxBytes <= 0) {
            return null;
        }

        return ((double) oldGenUsedBytes / oldGenMaxBytes) * 100;
    }
}
