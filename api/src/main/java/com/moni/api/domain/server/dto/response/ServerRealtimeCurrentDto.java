package com.moni.api.domain.server.dto.response;

import static com.moni.api.domain.server.mapper.ServerRealtimeMetricMapper.isValidEndpointUri;

import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerRealtimeCurrentDto {

    private LocalDateTime collectedAt;
    private Double processUptimeSeconds;
    private List<HttpEndpointDto> httpEndpoints;
    private List<HikariCpPoolDto> hikaricpPools;
    private List<ExecutorDto> executors;

    public static ServerRealtimeCurrentDto from(ServerRealtimeMetric metric) {
        return from(metric, null);
    }

    public static ServerRealtimeCurrentDto from(ServerRealtimeMetric metric, ServerRealtimeMetric previousMetric) {
        if (metric == null) {
            return null;
        }

        Double uptime = metric.getJvmMetric() != null ? metric.getJvmMetric().getProcessUptimeSeconds() : 0.0;

        List<HttpEndpointDto> endpoints;
        if (metric.getHttpEndpoints() != null) {
            Map<String, Long> prevCounts = new LinkedHashMap<>();
            long secDiff = 0;
            if (previousMetric != null && previousMetric.getCollectedAt() != null && metric.getCollectedAt() != null) {
                secDiff = Duration.between(previousMetric.getCollectedAt(), metric.getCollectedAt()).getSeconds();
                if (previousMetric.getHttpEndpoints() != null) {
                    for (var prevEp : previousMetric.getHttpEndpoints()) {
                        prevCounts.put(prevEp.getUri() + "|" + prevEp.getMethod(),
                                prevEp.getRequestsCount() != null ? prevEp.getRequestsCount() : 0L);
                    }
                }
            }

            final long finalSecDiff = secDiff;
            endpoints = metric.getHttpEndpoints().stream()
                    .filter(ep -> isValidEndpointUri(ep.getUri()))
                    .map(ep -> {
                String key = ep.getUri() + "|" + ep.getMethod();
                Long prevCount = prevCounts.get(key);
                long curCount = ep.getRequestsCount() != null ? ep.getRequestsCount() : 0L;
                double rps = 0.0;
                if (prevCount != null && finalSecDiff > 0 && curCount >= prevCount) {
                    rps = (double) (curCount - prevCount) / finalSecDiff;
                }
                return HttpEndpointDto.of(ep, rps);
            }).collect(Collectors.toList());
        } else {
            endpoints = Collections.emptyList();
        }

        List<HikariCpPoolDto> pools = metric.getHikaricpPools() != null
                ? metric.getHikaricpPools().stream().map(HikariCpPoolDto::from).collect(Collectors.toList())
                : Collections.emptyList();

        List<ExecutorDto> execs = metric.getExecutors() != null
                ? metric.getExecutors().stream().map(ExecutorDto::from).collect(Collectors.toList())
                : Collections.emptyList();

        return ServerRealtimeCurrentDto.builder()
                .collectedAt(metric.getCollectedAt())
                .processUptimeSeconds(uptime)
                .httpEndpoints(endpoints)
                .hikaricpPools(pools)
                .executors(execs)
                .build();
    }
}
