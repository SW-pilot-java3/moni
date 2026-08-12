package com.moni.api.domain.instance.enums;

import lombok.Getter;

@Getter
public enum MetricKey {
    CPU_USAGE(80.0, 95.0),
    MEM_USAGE(80.0, 95.0),
    DISK_USAGE(80.0, 95.0),
    DISK_LATENCY(100.0, 500.0),
    NET_ERROR_RATE(1.0, 5.0);

    private final double defaultWarningVal;
    private final double defaultCriticalVal;

    MetricKey(double defaultWarningVal, double defaultCriticalVal) {
        this.defaultWarningVal = defaultWarningVal;
        this.defaultCriticalVal = defaultCriticalVal;
    }
}