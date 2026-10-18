package com.moni.api.global.threshold;

import java.time.Duration;
import java.time.LocalDateTime;

public final class CooldownPolicy {

    private static final Duration CRITICAL_COOLDOWN = Duration.ofMinutes(15);
    private static final Duration WARNING_COOLDOWN = Duration.ofHours(1);

    private CooldownPolicy() {
    }

    public static boolean shouldSuppress(ThresholdSeverity lastSeverity, LocalDateTime lastCollectedAt,
            ThresholdSeverity currentSeverity, LocalDateTime currentCollectedAt) {
        if (lastSeverity == ThresholdSeverity.WARNING && currentSeverity == ThresholdSeverity.CRITICAL) {
            return false;
        }

        Duration cooldown = currentSeverity == ThresholdSeverity.CRITICAL ? CRITICAL_COOLDOWN : WARNING_COOLDOWN;
        return Duration.between(lastCollectedAt, currentCollectedAt).compareTo(cooldown) < 0;
    }
}
