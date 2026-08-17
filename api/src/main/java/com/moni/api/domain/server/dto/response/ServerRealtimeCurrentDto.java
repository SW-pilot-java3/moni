package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
        if (metric == null) {
            return null;
        }

        Double uptime = metric.getJvmMetric() != null ? metric.getJvmMetric().getProcessUptimeSeconds() : 0.0;

        List<HttpEndpointDto> endpoints = metric.getHttpEndpoints() != null
                ? metric.getHttpEndpoints().stream().map(HttpEndpointDto::from).collect(Collectors.toList())
                : Collections.emptyList();

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
