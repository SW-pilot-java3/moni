package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class NetErrorRateCalculator {

    private NetErrorRateCalculator() {
    }

    static Double calculate(List<InstanceNetworkMetric> previous, List<InstanceNetworkMetric> current) {
        if (previous == null || current == null) {
            return null;
        }

        Map<String, InstanceNetworkMetric> previousByInterface = previous.stream()
                .collect(Collectors.toMap(InstanceNetworkMetric::getInterfaceName, Function.identity(), (a, b) -> a));

        Double maxErrorsPerSec = null;

        for (InstanceNetworkMetric currentInterface : current) {
            InstanceNetworkMetric previousInterface = previousByInterface.get(currentInterface.getInterfaceName());
            if (previousInterface == null) {
                continue;
            }

            Double errorsPerSec = calculateInterfaceErrorRate(previousInterface, currentInterface);
            if (errorsPerSec != null && (maxErrorsPerSec == null || errorsPerSec > maxErrorsPerSec)) {
                maxErrorsPerSec = errorsPerSec;
            }
        }

        return maxErrorsPerSec;
    }

    private static Double calculateInterfaceErrorRate(InstanceNetworkMetric previous, InstanceNetworkMetric current) {
        Long prevRxErrors = previous.getRxErrorsTotal();
        Long prevTxErrors = previous.getTxErrorsTotal();
        Long currRxErrors = current.getRxErrorsTotal();
        Long currTxErrors = current.getTxErrorsTotal();

        if (prevRxErrors == null || prevTxErrors == null || currRxErrors == null || currTxErrors == null) {
            return null;
        }

        long secondsElapsed = Duration.between(previous.getCollectedAt(), current.getCollectedAt()).getSeconds();
        if (secondsElapsed <= 0) {
            return null;
        }

        long errorsDelta = (currRxErrors - prevRxErrors) + (currTxErrors - prevTxErrors);
        if (errorsDelta < 0) {
            return null;
        }

        return (double) errorsDelta / secondsElapsed;
    }
}
