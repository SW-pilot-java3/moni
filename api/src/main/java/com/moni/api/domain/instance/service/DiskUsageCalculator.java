package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;

import java.util.List;

final class DiskUsageCalculator {

    private DiskUsageCalculator() {
    }

    static Double calculate(List<InstanceFileSystemMetric> filesystems) {
        if (filesystems == null) {
            return null;
        }

        Double maxUsagePct = null;

        for (InstanceFileSystemMetric filesystem : filesystems) {
            Long fsSizeBytes = filesystem.getFsSizeBytes();
            Long fsAvailBytes = filesystem.getFsAvailBytes();

            if (fsSizeBytes == null || fsSizeBytes <= 0 || fsAvailBytes == null) {
                continue;
            }

            double usagePct = (1 - ((double) fsAvailBytes / fsSizeBytes)) * 100;
            if (maxUsagePct == null || usagePct > maxUsagePct) {
                maxUsagePct = usagePct;
            }
        }

        return maxUsagePct;
    }
}
