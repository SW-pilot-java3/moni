package com.moni.api.global.threshold;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThresholdSeverityResolverTest {

    @Test
    @DisplayName("critical 이상이면 CRITICAL을 반환한다")
    void resolve_valueAboveCritical_returnsCritical() {
        assertThat(ThresholdSeverityResolver.resolve(95.0, 80.0, 90.0)).isEqualTo(ThresholdSeverity.CRITICAL);
    }

    @Test
    @DisplayName("critical과 같으면 CRITICAL을 반환한다")
    void resolve_valueEqualsCritical_returnsCritical() {
        assertThat(ThresholdSeverityResolver.resolve(90.0, 80.0, 90.0)).isEqualTo(ThresholdSeverity.CRITICAL);
    }

    @Test
    @DisplayName("warning 이상 critical 미만이면 WARNING을 반환한다")
    void resolve_valueBetweenWarningAndCritical_returnsWarning() {
        assertThat(ThresholdSeverityResolver.resolve(85.0, 80.0, 90.0)).isEqualTo(ThresholdSeverity.WARNING);
    }

    @Test
    @DisplayName("warning과 같으면 WARNING을 반환한다")
    void resolve_valueEqualsWarning_returnsWarning() {
        assertThat(ThresholdSeverityResolver.resolve(80.0, 80.0, 90.0)).isEqualTo(ThresholdSeverity.WARNING);
    }

    @Test
    @DisplayName("warning 미만이면 null(정상)을 반환한다")
    void resolve_valueBelowWarning_returnsNull() {
        assertThat(ThresholdSeverityResolver.resolve(50.0, 80.0, 90.0)).isNull();
    }

    @Test
    @DisplayName("value가 null이면 null을 반환한다")
    void resolve_nullValue_returnsNull() {
        assertThat(ThresholdSeverityResolver.resolve(null, 80.0, 90.0)).isNull();
    }

    @Test
    @DisplayName("warningVal이 null이면 null을 반환한다")
    void resolve_nullWarningVal_returnsNull() {
        assertThat(ThresholdSeverityResolver.resolve(95.0, null, 90.0)).isNull();
    }

    @Test
    @DisplayName("criticalVal이 null이면 null을 반환한다")
    void resolve_nullCriticalVal_returnsNull() {
        assertThat(ThresholdSeverityResolver.resolve(95.0, 80.0, null)).isNull();
    }
}
