package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceNetworkMetric;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class NetworkUsageCalculator {

    private NetworkUsageCalculator() {
    }

    record Result(Double rxBytesPerSec, Double txBytesPerSec, Double errorsPerSec) {
        static final Result EMPTY = new Result(null, null, null);
    }

    static Result calculate(List<InstanceNetworkMetric> previous, List<InstanceNetworkMetric> current, LocalDateTime previousCollectedAt, LocalDateTime currentCollectedAt) {
        if (previous == null || previous.isEmpty() || current == null || current.isEmpty()) {
            return Result.EMPTY;
        }

        double intervalSeconds = ChronoUnit.MILLIS.between(previousCollectedAt, currentCollectedAt) / 1000.0;
        if (intervalSeconds <= 0) {
            return Result.EMPTY;
        }

        Map<String, InstanceNetworkMetric> previousByInterface = previous.stream()
                .collect(Collectors.toMap(InstanceNetworkMetric::getInterfaceName, Function.identity(), (a, b) -> a));

        long rxBytesDelta = 0;
        long txBytesDelta = 0;
        long errorsDelta = 0;
        boolean hasBytes = false;
        boolean hasErrors = false;

        for (InstanceNetworkMetric currentInterface : current) {
            InstanceNetworkMetric previousInterface = previousByInterface.get(currentInterface.getInterfaceName());
            if (previousInterface == null) {
                continue;
            }

            if (currentInterface.getRxBytesTotal() != null && previousInterface.getRxBytesTotal() != null) {
                rxBytesDelta += Math.max(0, currentInterface.getRxBytesTotal() - previousInterface.getRxBytesTotal());
                hasBytes = true;
            }
            if (currentInterface.getTxBytesTotal() != null && previousInterface.getTxBytesTotal() != null) {
                txBytesDelta += Math.max(0, currentInterface.getTxBytesTotal() - previousInterface.getTxBytesTotal());
                hasBytes = true;
            }
            if (currentInterface.getRxErrorsTotal() != null && previousInterface.getRxErrorsTotal() != null) {
                errorsDelta += Math.max(0, currentInterface.getRxErrorsTotal() - previousInterface.getRxErrorsTotal());
                hasErrors = true;
            }
            if (currentInterface.getTxErrorsTotal() != null && previousInterface.getTxErrorsTotal() != null) {
                errorsDelta += Math.max(0, currentInterface.getTxErrorsTotal() - previousInterface.getTxErrorsTotal());
                hasErrors = true;
            }
        }

        Double rxBytesPerSec = hasBytes ? rxBytesDelta / intervalSeconds : null;
        Double txBytesPerSec = hasBytes ? txBytesDelta / intervalSeconds : null;
        Double errorsPerSec = hasErrors ? errorsDelta / intervalSeconds : null;

        return new Result(rxBytesPerSec, txBytesPerSec, errorsPerSec);
    }
}