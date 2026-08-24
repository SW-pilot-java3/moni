package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class HttpErrorRateCalculator {

    private HttpErrorRateCalculator() {
    }

    static Double calculate(List<ServerHttpEndpointMetric> previous, List<ServerHttpEndpointMetric> current) {
        if (previous == null || current == null) {
            return null;
        }

        Map<String, ServerHttpEndpointMetric> previousByEndpoint = previous.stream()
                .collect(Collectors.toMap(HttpErrorRateCalculator::endpointKey, Function.identity(), (a, b) -> a));

        long totalCountDelta = 0;
        long errorCountDelta = 0;
        boolean hasMatch = false;

        for (ServerHttpEndpointMetric currentEndpoint : current) {
            ServerHttpEndpointMetric previousEndpoint = previousByEndpoint.get(endpointKey(currentEndpoint));
            if (previousEndpoint == null) {
                continue; // 신규 등장 엔드포인트, 이번 윈도우 판정 스킵
            }

            Long prevCount = previousEndpoint.getRequestsCount();
            Long currCount = currentEndpoint.getRequestsCount();
            if (prevCount == null || currCount == null) {
                continue;
            }

            long countDelta = currCount - prevCount;
            if (countDelta <= 0) {
                continue;
            }

            totalCountDelta += countDelta;
            if (isServerError(currentEndpoint.getStatus())) {
                errorCountDelta += countDelta;
            }
            hasMatch = true;
        }

        if (!hasMatch || totalCountDelta <= 0) {
            return null;
        }

        return ((double) errorCountDelta / totalCountDelta) * 100;
    }

    private static boolean isServerError(String status) {
        return status != null && status.startsWith("5");
    }

    private static String endpointKey(ServerHttpEndpointMetric endpoint) {
        return endpoint.getUri() + ":" + endpoint.getMethod() + ":" + endpoint.getStatus();
    }
}
