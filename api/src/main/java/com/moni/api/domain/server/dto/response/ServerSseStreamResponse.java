package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerSseStreamResponse {

    private Long serverId;
    private LocalDateTime collectedAt;
    private ServerMetricSummaryDto summary;
    private ServerJvmMetricsDto jvm;
    private List<ServerHttpMetricsDto> httpEndpoints;
    private List<ServerHikariMetricsDto> hikaricpPools;
    private List<ServerExecutorMetricsDto> executors;

    @Getter
    @Builder
    public static class ServerMetricSummaryDto {
        private Double totalRps;
        private Double avgLatencyMs;
        private Long jvmHeapUsedBytes;
        private Long jvmHeapMaxBytes;
        private Integer hikaricpActiveTotal;
        private Integer hikaricpMaxTotal;
        private Integer executorActiveTotal;
        private Integer executorMaxTotal;
    }

    @Getter
    @Builder
    public static class ServerJvmMetricsDto {
        private Long heapUsedBytes;
        private Long heapMaxBytes;
        private Integer threadsLive;
        private Integer threadsBlocked;
        private Double gcPauseSecondsSum;
    }

    @Getter
    @Builder
    public static class ServerHttpMetricsDto {
        private String uri;
        private String method;
        private String status;
        private Long requestsCount;
        private Double rps;
        private Double avgLatencyMs;
        private Double maxLatencyMs;
        private Double errorRatePct;
    }

    @Getter
    @Builder
    public static class ServerHikariMetricsDto {
        private String poolName;
        private Integer active;
        private Integer idle;
        private Integer pending;
        private Integer max;
    }

    @Getter
    @Builder
    public static class ServerExecutorMetricsDto {
        private String name;
        private Integer active;
        private Integer max;
        private Integer queuedTasks;
        private Integer queueRemaining;
    }
}
