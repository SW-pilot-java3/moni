package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class HttpAvgLatencyCalculator {

    private HttpAvgLatencyCalculator() {
    }

    static Double calculate(List<ServerHttpEndpointMetric> previous, List<ServerHttpEndpointMetric> current) {
        if (previous == null || current == null) {
            return null;
        }

        Map<String, ServerHttpEndpointMetric> previousByEndpoint = previous.stream()
                .collect(Collectors.toMap(HttpAvgLatencyCalculator::endpointKey, Function.identity(), (a, b) -> a));

        double totalSumDelta = 0;
        long totalCountDelta = 0;
        boolean hasMatch = false;

        for (ServerHttpEndpointMetric currentEndpoint : current) {
            ServerHttpEndpointMetric previousEndpoint = previousByEndpoint.get(endpointKey(currentEndpoint));
            if (previousEndpoint == null) {
                continue; // 신규 등장 엔드포인트, 이번 윈도우 판정 스킵
            }

            Long prevCount = previousEndpoint.getRequestsCount();
            Double prevSum = previousEndpoint.getRequestsSum();
            Long currCount = currentEndpoint.getRequestsCount();
            Double currSum = currentEndpoint.getRequestsSum();

            if (prevCount == null || prevSum == null || currCount == null || currSum == null) {
                continue;
            }

            long countDelta = currCount - prevCount;
            double sumDelta = currSum - prevSum;
            if (countDelta <= 0 || sumDelta < 0) {
                continue;
            }

            totalCountDelta += countDelta;
            totalSumDelta += sumDelta;
            hasMatch = true;
        }

        if (!hasMatch || totalCountDelta <= 0) {
            return null;
        }

        return (totalSumDelta / totalCountDelta) * 1000;
    }

    private static String endpointKey(ServerHttpEndpointMetric endpoint) {
        return endpoint.getUri() + ":" + endpoint.getMethod() + ":" + endpoint.getStatus();
    }
}
