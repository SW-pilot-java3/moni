package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.global.threshold.ThresholdSeverityResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
class ServerAnomalyDebounceEvaluator {

    private static final int STREAK_LENGTH = 3;

    private final ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    boolean isConsecutivelyExceeded(Long serverId, ServerMetricKey metricKey, ServerThreshold threshold) {
        List<Double> recentValues = switch (metricKey) {
            case JVM_HEAP_USAGE -> recentGaugeValues(serverId, row -> JvmHeapUsageCalculator.calculate(row.getJvmMetric()));
            case JVM_OLD_GEN_USAGE -> recentGaugeValues(serverId, row -> JvmOldGenUsageCalculator.calculate(row.getJvmMetric()));
            case HIKARICP_POOL_USAGE -> recentGaugeValues(serverId, row -> HikariPoolUsageCalculator.calculate(row.getHikaricpPools()));
            case THREADPOOL_QUEUE_USAGE -> recentGaugeValues(serverId, row -> ThreadPoolQueueUsageCalculator.calculate(row.getExecutors()));
            case GC_PAUSE_TIME -> recentGcPauseTimeValues(serverId);
            case HTTP_AVG_LATENCY -> recentHttpEndpointValues(serverId, HttpAvgLatencyCalculator::calculate);
            case HTTP_ERROR_RATE -> recentHttpEndpointValues(serverId, HttpErrorRateCalculator::calculate);
        };

        if (recentValues.size() < STREAK_LENGTH) {
            return false;
        }

        return recentValues.stream().allMatch(value -> ThresholdSeverityResolver.resolve(
                value, threshold.getWarningValue(), threshold.getCriticalValue()) != null);
    }

    private List<Double> recentGaugeValues(Long serverId, java.util.function.Function<ServerRealtimeMetric, Double> calculate) {
        List<ServerRealtimeMetric> rows = serverRealtimeMetricRepository
                .findByServerIdOrderByCollectedAtDesc(serverId, PageRequest.of(0, STREAK_LENGTH));

        return rows.stream()
                .map(calculate)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Double> recentGcPauseTimeValues(Long serverId) {
        List<ServerRealtimeMetric> rows = serverRealtimeMetricRepository
                .findByServerIdOrderByCollectedAtDesc(serverId, PageRequest.of(0, STREAK_LENGTH + 1));

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < rows.size() - 1 && values.size() < STREAK_LENGTH; i++) {
            Double value = GcPauseTimeCalculator.calculate(rows.get(i + 1).getJvmMetric(), rows.get(i).getJvmMetric());
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Double> recentHttpEndpointValues(Long serverId,
            BiFunction<Collection<ServerHttpEndpointMetric>, Collection<ServerHttpEndpointMetric>, Double> calculate) {
        List<ServerRealtimeMetric> rows = serverRealtimeMetricRepository
                .findByServerIdOrderByCollectedAtDesc(serverId, PageRequest.of(0, STREAK_LENGTH + 1));

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < rows.size() - 1 && values.size() < STREAK_LENGTH; i++) {
            Double value = calculate.apply(rows.get(i + 1).getHttpEndpoints(), rows.get(i).getHttpEndpoints());
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }
}
