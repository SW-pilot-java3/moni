package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HttpEndpointDto {

    private String uri;
    private String method;
    private String status;
    private Long requestsCount;
    private Double rps;
    private Double avgLatencyMs;
    private Double maxLatencyMs;
    private Double errorRatePct;

    public static HttpEndpointDto from(ServerHttpEndpointMetric metric) {
        long count = metric.getRequestsCount() != null ? metric.getRequestsCount() : 0L;
        double sum = metric.getRequestsSum() != null ? metric.getRequestsSum() : 0.0;
        double max = metric.getRequestsMax() != null ? metric.getRequestsMax() : 0.0;

        double avgLatency = count > 0 ? (sum / count) * 1000.0 : 0.0;
        double maxLatency = max * 1000.0;

        boolean isError = metric.getStatus() != null && (metric.getStatus().startsWith("4") || metric.getStatus().startsWith("5"));
        double errorRate = isError ? 100.0 : 0.0;

        return HttpEndpointDto.builder()
                .uri(metric.getUri())
                .method(metric.getMethod())
                .status(metric.getStatus())
                .requestsCount(count)
                .rps(0.0)
                .avgLatencyMs(Math.round(avgLatency * 10.0) / 10.0)
                .maxLatencyMs(Math.round(maxLatency * 10.0) / 10.0)
                .errorRatePct(errorRate)
                .build();
    }

    public static HttpEndpointDto of(ServerHttpEndpointMetric metric, Double calculatedRps) {
        HttpEndpointDto dto = from(metric);
        return HttpEndpointDto.builder()
                .uri(dto.getUri())
                .method(dto.getMethod())
                .status(dto.getStatus())
                .requestsCount(dto.getRequestsCount())
                .rps(calculatedRps != null ? Math.round(calculatedRps * 10.0) / 10.0 : 0.0)
                .avgLatencyMs(dto.getAvgLatencyMs())
                .maxLatencyMs(dto.getMaxLatencyMs())
                .errorRatePct(dto.getErrorRatePct())
                .build();
    }
}
