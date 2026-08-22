package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;

import java.util.List;

final class HikariPoolUsageCalculator {

    private HikariPoolUsageCalculator() {
    }

    static Double calculate(List<ServerHikariCpPoolMetric> pools) {
        if (pools == null) {
            return null;
        }

        Double maxUsagePct = null;

        for (ServerHikariCpPoolMetric pool : pools) {
            Integer active = pool.getActive();
            Integer max = pool.getMax();

            if (active == null || max == null || max <= 0) {
                continue;
            }

            double usagePct = ((double) active / max) * 100;
            if (maxUsagePct == null || usagePct > maxUsagePct) {
                maxUsagePct = usagePct;
            }
        }

        return maxUsagePct;
    }
}
