package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;

public record InstanceThresholdResponse(
        MetricKey metricKey,
        Double warningValue,
        Double criticalValue,
        boolean isCustomized
) {
    public static InstanceThresholdResponse from(InstanceThreshold threshold) {
        return new InstanceThresholdResponse(
                threshold.getMetricKey(),
                threshold.getWarningVal(),
                threshold.getCriticalVal(),
                true
        );
    }

    public static InstanceThresholdResponse defaultOf(MetricKey metricKey) {
        return new InstanceThresholdResponse(
                metricKey,
                metricKey.getDefaultWarningVal(),
                metricKey.getDefaultCriticalVal(),
                false
        );
    }
}
