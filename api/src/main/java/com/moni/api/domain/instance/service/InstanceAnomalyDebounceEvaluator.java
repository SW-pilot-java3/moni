package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.repository.InstanceDiskMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceFileSystemMetricRepository;
import com.moni.api.domain.instance.repository.InstanceNetworkMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import com.moni.api.global.threshold.ThresholdSeverityResolver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
class InstanceAnomalyDebounceEvaluator {

    private static final int STREAK_LENGTH = 3;

    private final InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;
    private final InstanceFileSystemMetricRepository instanceFileSystemMetricRepository;
    private final InstanceDiskMetricsRepository instanceDiskMetricsRepository;
    private final InstanceNetworkMetricsRepository instanceNetworkMetricsRepository;

    boolean isConsecutivelyExceeded(Long instanceId, MetricKey metricKey, InstanceThreshold threshold) {
        List<Double> recentValues = switch (metricKey) {
            case CPU_USAGE -> recentCpuUsageValues(instanceId);
            case MEM_USAGE -> recentMemUsageValues(instanceId);
            case DISK_USAGE -> recentDiskUsageValues(instanceId);
            case DISK_LATENCY -> recentDiskLatencyValues(instanceId);
            case NET_ERROR_RATE -> recentNetErrorRateValues(instanceId);
        };

        if (recentValues.size() < STREAK_LENGTH) {
            return false;
        }

        return recentValues.stream().allMatch(value -> ThresholdSeverityResolver.resolve(
                value, threshold.getWarningVal(), threshold.getCriticalVal()) != null);
    }

    private List<Double> recentCpuUsageValues(Long instanceId) {
        List<InstanceRealtimeMetric> rows = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, STREAK_LENGTH + 1));

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < rows.size() - 1 && values.size() < STREAK_LENGTH; i++) {
            Double value = CpuUsageCalculator.calculate(rows.get(i + 1).getCpuMetrics(), rows.get(i).getCpuMetrics());
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Double> recentMemUsageValues(Long instanceId) {
        List<InstanceRealtimeMetric> rows = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, STREAK_LENGTH));

        return rows.stream()
                .map(row -> MemoryUsageCalculator.calculate(row.getMemoryMetrics()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Double> recentDiskUsageValues(Long instanceId) {
        List<InstanceRealtimeMetric> rows = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, STREAK_LENGTH));
        if (rows.isEmpty()) {
            return List.of();
        }

        LocalDateTime from = rows.get(rows.size() - 1).getCollectedAt();
        LocalDateTime to = rows.get(0).getCollectedAt();
        Map<LocalDateTime, List<InstanceFileSystemMetric>> byCollectedAt = instanceFileSystemMetricRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to).stream()
                .collect(Collectors.groupingBy(InstanceFileSystemMetric::getCollectedAt));

        List<Double> values = new ArrayList<>();
        for (InstanceRealtimeMetric row : rows) {
            Double value = DiskUsageCalculator.calculate(byCollectedAt.get(row.getCollectedAt()));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Double> recentDiskLatencyValues(Long instanceId) {
        List<InstanceRealtimeMetric> rows = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, STREAK_LENGTH + 1));
        if (rows.size() < 2) {
            return List.of();
        }

        LocalDateTime from = rows.get(rows.size() - 1).getCollectedAt();
        LocalDateTime to = rows.get(0).getCollectedAt();
        Map<LocalDateTime, List<InstanceDiskMetric>> byCollectedAt = instanceDiskMetricsRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to).stream()
                .collect(Collectors.groupingBy(InstanceDiskMetric::getCollectedAt));

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < rows.size() - 1 && values.size() < STREAK_LENGTH; i++) {
            List<InstanceDiskMetric> previous = byCollectedAt.get(rows.get(i + 1).getCollectedAt());
            List<InstanceDiskMetric> current = byCollectedAt.get(rows.get(i).getCollectedAt());
            Double value = DiskLatencyCalculator.calculate(previous, current);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Double> recentNetErrorRateValues(Long instanceId) {
        List<InstanceRealtimeMetric> rows = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, STREAK_LENGTH + 1));
        if (rows.size() < 2) {
            return List.of();
        }

        LocalDateTime from = rows.get(rows.size() - 1).getCollectedAt();
        LocalDateTime to = rows.get(0).getCollectedAt();
        Map<LocalDateTime, List<InstanceNetworkMetric>> byCollectedAt = instanceNetworkMetricsRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to).stream()
                .collect(Collectors.groupingBy(InstanceNetworkMetric::getCollectedAt));

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < rows.size() - 1 && values.size() < STREAK_LENGTH; i++) {
            List<InstanceNetworkMetric> previous = byCollectedAt.get(rows.get(i + 1).getCollectedAt());
            List<InstanceNetworkMetric> current = byCollectedAt.get(rows.get(i).getCollectedAt());
            Double value = NetErrorRateCalculator.calculate(previous, current);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }
}
