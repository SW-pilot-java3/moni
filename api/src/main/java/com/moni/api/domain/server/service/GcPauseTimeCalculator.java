package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.JvmMetric;

final class GcPauseTimeCalculator {

    private GcPauseTimeCalculator() {
    }

    static Double calculate(JvmMetric previous, JvmMetric current) {
        if (previous == null || current == null) {
            return null;
        }

        Long prevCount = previous.getGcPauseSecondsCount();
        Double prevSum = previous.getGcPauseSecondsSum();
        Long currCount = current.getGcPauseSecondsCount();
        Double currSum = current.getGcPauseSecondsSum();

        if (prevCount == null || prevSum == null || currCount == null || currSum == null) {
            return null;
        }

        long countDelta = currCount - prevCount;
        double sumDelta = currSum - prevSum;

        if (countDelta <= 0 || sumDelta < 0) {
            return null;
        }

        return sumDelta / countDelta;
    }
}
