package com.moni.api.domain.server.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerHistorySummaryDto {

    private JvmSummary jvm;
    private List<HttpEndpointSummary> httpEndpoints;
    private List<HikariCpPoolSummary> hikaricpPools;
    private List<ThreadPoolSummary> executors;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JvmSummary {
        private Long heapUsedAvgBytes;
        private Long heapUsedMaxBytes;
        private Long oldGenUsedAvgBytes;
        private Long gcPauseCountSum;
        private Double gcPauseSecondsSum;
        private Integer threadBlockedMax;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HttpEndpointSummary {
        private String uri;
        private String method;
        private Long totalRequestsCount;
        private Double rpsAvg;
        private Double rpsMax;
        private Double avgResTimeMs;
        private Double maxResTimeMs;
        private Double errorRateAvg;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HikariCpPoolSummary {
        private String poolName;
        private Double activePoolAvg;
        private Integer activePoolMax;
        private Integer pendingThreadsMax;
        private Integer timeoutCountSum;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThreadPoolSummary {
        private String name;
        private Double activeThreadsAvg;
        private Double maxThreadsAvg;
        private Double queuedTasksAvg;
        private Integer queuedTasksMax;
    }
}
