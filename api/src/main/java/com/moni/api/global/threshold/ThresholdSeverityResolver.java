package com.moni.api.global.threshold;

public final class ThresholdSeverityResolver {

    private ThresholdSeverityResolver() {
    }

    public static ThresholdSeverity resolve(Double value, Double warningVal, Double criticalVal) {
        if (value == null || warningVal == null || criticalVal == null) {
            return null;
        }

        if (value >= criticalVal) {
            return ThresholdSeverity.CRITICAL;
        }
        if (value >= warningVal) {
            return ThresholdSeverity.WARNING;
        }

        return null;
    }
}
