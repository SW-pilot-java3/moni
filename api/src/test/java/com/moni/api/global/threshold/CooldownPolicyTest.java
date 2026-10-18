package com.moni.api.global.threshold;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CooldownPolicyTest {

    @Test
    @DisplayName("CRITICAL은 마지막 리포트로부터 15분 이내면 억제한다")
    void shouldSuppress_criticalWithin15Minutes_returnsTrue() {
        LocalDateTime last = LocalDateTime.now();
        LocalDateTime current = last.plusMinutes(10);

        boolean result = CooldownPolicy.shouldSuppress(
                ThresholdSeverity.CRITICAL, last, ThresholdSeverity.CRITICAL, current);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("CRITICAL은 마지막 리포트로부터 15분이 지나면 억제하지 않는다")
    void shouldSuppress_criticalAfter15Minutes_returnsFalse() {
        LocalDateTime last = LocalDateTime.now();
        LocalDateTime current = last.plusMinutes(16);

        boolean result = CooldownPolicy.shouldSuppress(
                ThresholdSeverity.CRITICAL, last, ThresholdSeverity.CRITICAL, current);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("WARNING은 마지막 리포트로부터 1시간 이내면 억제한다")
    void shouldSuppress_warningWithin1Hour_returnsTrue() {
        LocalDateTime last = LocalDateTime.now();
        LocalDateTime current = last.plusMinutes(30);

        boolean result = CooldownPolicy.shouldSuppress(
                ThresholdSeverity.WARNING, last, ThresholdSeverity.WARNING, current);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("WARNING에서 CRITICAL로 악화되면 쿨다운 중이어도 억제하지 않는다")
    void shouldSuppress_escalationFromWarningToCritical_returnsFalse() {
        LocalDateTime last = LocalDateTime.now();
        LocalDateTime current = last.plusMinutes(1);

        boolean result = CooldownPolicy.shouldSuppress(
                ThresholdSeverity.WARNING, last, ThresholdSeverity.CRITICAL, current);

        assertThat(result).isFalse();
    }
}
