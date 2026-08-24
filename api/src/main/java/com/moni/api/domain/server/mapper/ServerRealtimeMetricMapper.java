package com.moni.api.domain.server.mapper;

import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.server.dto.response.ServerSseStreamResponse;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerRealtimeMetricMapper {

    @FunctionalInterface
    public interface EndpointRpsCalculator {
        Double calculate(String uri, String method, long currentCount);
    }

    public static boolean isValidEndpointUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        String trimmed = uri.trim();
        return !trimmed.equalsIgnoreCase("UNKNOWN") && !trimmed.equalsIgnoreCase("NOT_FOUND");
    }

    public static ServerRealtimeMetric toEntity(Long serverId, LocalDateTime collectedAt,
            MetricRecordRequest.ServerMetricPayload payload) {
        if (payload == null) {
            return null;
        }

        JvmMetric jvmMetric = JvmMetric.builder()
                .jvmHeapUsedBytes(payload.getJvmHeapUsedBytes())
                .jvmHeapMaxBytes(payload.getJvmHeapMaxBytes())
                .jvmOldGenUsedBytes(payload.getJvmOldGenUsedBytes())
                .jvmOldGenMaxBytes(payload.getJvmOldGenMaxBytes())
                .gcPauseSecondsCount(payload.getGcPauseSecondsCount())
                .gcPauseSecondsSum(payload.getGcPauseSecondsSum())
                .processUptimeSeconds(payload.getProcessUptimeSeconds())
                .jvmThreadsLive(payload.getJvmThreadsLive())
                .jvmThreadsBlocked(payload.getJvmThreadsBlocked())
                .build();

        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetric.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .jvmMetric(jvmMetric)
                .build();

        if (payload.getHttpEndpoints() != null) {
            List<ServerHttpEndpointMetric> endpoints = payload.getHttpEndpoints().stream()
                    .filter(ep -> isValidEndpointUri(ep.getUri()))
                    .map(ep -> ServerHttpEndpointMetric.builder()
                            .realtimeMetric(realtimeMetric)
                            .serverId(serverId)
                            .collectedAt(collectedAt)
                            .uri(ep.getUri())
                            .method(ep.getMethod())
                            .status(ep.getStatus())
                            .requestsCount(ep.getRequestsCount())
                            .requestsSum(ep.getRequestsSum())
                            .requestsMax(ep.getRequestsMax())
                            .build())
                    .collect(Collectors.toList());
            realtimeMetric.getHttpEndpoints().addAll(endpoints);
        }

        if (payload.getHikaricpPools() != null) {
            List<ServerHikariCpPoolMetric> pools = payload.getHikaricpPools().stream()
                    .map(pool -> ServerHikariCpPoolMetric.builder()
                            .realtimeMetric(realtimeMetric)
                            .serverId(serverId)
                            .collectedAt(collectedAt)
                            .poolName(pool.getPoolName())
                            .active(pool.getActive())
                            .idle(pool.getIdle())
                            .pending(pool.getPending())
                            .max(pool.getMax())
                            .timeoutsTotal(pool.getTimeoutsTotal())
                            .build())
                    .collect(Collectors.toList());
            realtimeMetric.getHikaricpPools().addAll(pools);
        }

        if (payload.getExecutors() != null) {
            List<ServerExecutorMetric> executors = payload.getExecutors().stream()
                    .map(exec -> ServerExecutorMetric.builder()
                            .realtimeMetric(realtimeMetric)
                            .serverId(serverId)
                            .collectedAt(collectedAt)
                            .name(exec.getName())
                            .active(exec.getActive())
                            .max(exec.getMax())
                            .queuedTasks(exec.getQueuedTasks())
                            .queueRemaining(exec.getQueueRemaining())
                            .build())
                    .collect(Collectors.toList());
            realtimeMetric.getExecutors().addAll(executors);
        }

        return realtimeMetric;
    }

    public static ServerSseStreamResponse toSseResponse(Long serverId, LocalDateTime collectedAt,
            MetricRecordRequest.ServerMetricPayload payload) {
        return toSseResponse(serverId, collectedAt, payload, null);
    }

    public static ServerSseStreamResponse toSseResponse(Long serverId, LocalDateTime collectedAt,
            MetricRecordRequest.ServerMetricPayload payload,
            EndpointRpsCalculator rpsCalculator) {
        if (payload == null) {
            return null;
        }

        // 1. HTTP Endpoints
        List<ServerSseStreamResponse.ServerHttpMetricsDto> httpEndpoints;
        if (payload.getHttpEndpoints() != null) {
            Map<String, List<MetricRecordRequest.HttpEndpointPayload>> grouped = payload.getHttpEndpoints().stream()
                    .filter(ep -> isValidEndpointUri(ep.getUri()))
                    .collect(Collectors.groupingBy(
                            ep -> ep.getUri() + "|" + ep.getMethod(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            httpEndpoints = grouped.values().stream().map(group -> {
                MetricRecordRequest.HttpEndpointPayload first = group.get(0);
                long groupCount = group.stream().mapToLong(ep -> ep.getRequestsCount() != null ? ep.getRequestsCount() : 0L).sum();
                double groupSum = group.stream().mapToDouble(ep -> ep.getRequestsSum() != null ? ep.getRequestsSum() : 0.0).sum();
                double groupMax = group.stream().mapToDouble(ep -> ep.getRequestsMax() != null ? ep.getRequestsMax() : 0.0).max().orElse(0.0);

                long errorCount = group.stream()
                        .filter(ep -> ep.getStatus() != null && (ep.getStatus().startsWith("4") || ep.getStatus().startsWith("5")))
                        .mapToLong(ep -> ep.getRequestsCount() != null ? ep.getRequestsCount() : 0L)
                        .sum();

                double epAvgLatency = groupCount > 0 ? (groupSum / groupCount) * 1000.0 : 0.0;
                double epMaxLatency = groupMax * 1000.0;
                double errorRate = groupCount > 0 ? ((double) errorCount / groupCount) * 100.0 : 0.0;

                double epRps = (rpsCalculator != null)
                        ? rpsCalculator.calculate(first.getUri(), first.getMethod(), groupCount)
                        : 0.0;

                return ServerSseStreamResponse.ServerHttpMetricsDto.builder()
                        .uri(first.getUri())
                        .method(first.getMethod())
                        .requestsCount(groupCount)
                        .rps(epRps)
                        .avgLatencyMs(Math.round(epAvgLatency * 10.0) / 10.0)
                        .maxLatencyMs(Math.round(epMaxLatency * 10.0) / 10.0)
                        .errorRatePct(Math.round(errorRate * 10.0) / 10.0)
                        .build();
            }).collect(Collectors.toList());
        } else {
            httpEndpoints = Collections.emptyList();
        }

        // 2. Summary
        int hikaricpActiveTotal = (payload.getHikaricpPools() != null)
                ? payload.getHikaricpPools().stream().mapToInt(p -> p.getActive() != null ? p.getActive() : 0).sum()
                : 0;

        int hikaricpMaxTotal = (payload.getHikaricpPools() != null)
                ? payload.getHikaricpPools().stream().mapToInt(p -> p.getMax() != null ? p.getMax() : 0).sum()
                : 0;

        int executorActiveTotal = (payload.getExecutors() != null)
                ? payload.getExecutors().stream().mapToInt(e -> e.getActive() != null ? e.getActive() : 0).sum()
                : 0;

        int executorMaxTotal = (payload.getExecutors() != null)
                ? payload.getExecutors().stream().mapToInt(e -> e.getMax() != null ? e.getMax() : 0).sum()
                : 0;

        long totalRequestsCount = 0L;
        double totalRequestsSum = 0.0;
        if (payload.getHttpEndpoints() != null) {
            for (MetricRecordRequest.HttpEndpointPayload ep : payload.getHttpEndpoints()) {
                if (isValidEndpointUri(ep.getUri())) {
                    if (ep.getRequestsCount() != null) {
                        totalRequestsCount += ep.getRequestsCount();
                    }
                    if (ep.getRequestsSum() != null) {
                        totalRequestsSum += ep.getRequestsSum();
                    }
                }
            }
        }
        double avgLatencyMs = totalRequestsCount > 0
                ? Math.round((totalRequestsSum / totalRequestsCount) * 1000.0 * 10.0) / 10.0
                : 0.0;

        double totalRps = httpEndpoints.stream()
                .mapToDouble(ServerSseStreamResponse.ServerHttpMetricsDto::getRps)
                .sum();
        totalRps = Math.round(totalRps * 10.0) / 10.0;

        ServerSseStreamResponse.ServerMetricSummaryDto summary = ServerSseStreamResponse.ServerMetricSummaryDto.builder()
                .totalRps(totalRps)
                .avgLatencyMs(avgLatencyMs)
                .jvmHeapUsedBytes(payload.getJvmHeapUsedBytes())
                .jvmHeapMaxBytes(payload.getJvmHeapMaxBytes())
                .hikaricpActiveTotal(hikaricpActiveTotal)
                .hikaricpMaxTotal(hikaricpMaxTotal)
                .executorActiveTotal(executorActiveTotal)
                .executorMaxTotal(executorMaxTotal)
                .build();

        // 3. JVM
        ServerSseStreamResponse.ServerJvmMetricsDto jvm = ServerSseStreamResponse.ServerJvmMetricsDto.builder()
                .heapUsedBytes(payload.getJvmHeapUsedBytes())
                .heapMaxBytes(payload.getJvmHeapMaxBytes())
                .threadsLive(payload.getJvmThreadsLive())
                .threadsBlocked(payload.getJvmThreadsBlocked())
                .gcPauseSecondsSum(payload.getGcPauseSecondsSum())
                .build();

        // 4. HikariCP Pools
        List<ServerSseStreamResponse.ServerHikariMetricsDto> hikaricpPools = (payload.getHikaricpPools() != null)
                ? payload.getHikaricpPools().stream().map(p -> ServerSseStreamResponse.ServerHikariMetricsDto.builder()
                        .poolName(p.getPoolName())
                        .active(p.getActive())
                        .idle(p.getIdle())
                        .pending(p.getPending())
                        .max(p.getMax())
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        // 5. Executors
        List<ServerSseStreamResponse.ServerExecutorMetricsDto> executors = (payload.getExecutors() != null)
                ? payload.getExecutors().stream().map(e -> ServerSseStreamResponse.ServerExecutorMetricsDto.builder()
                        .name(e.getName())
                        .active(e.getActive())
                        .max(e.getMax())
                        .queuedTasks(e.getQueuedTasks())
                        .queueRemaining(e.getQueueRemaining())
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return ServerSseStreamResponse.builder()
                .serverId(serverId)
                .collectedAt(collectedAt)
                .summary(summary)
                .jvm(jvm)
                .httpEndpoints(httpEndpoints)
                .hikaricpPools(hikaricpPools)
                .executors(executors)
                .build();
    }
}
