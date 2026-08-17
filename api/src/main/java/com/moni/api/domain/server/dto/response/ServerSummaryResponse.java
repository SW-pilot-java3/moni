package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerSummaryResponse {

    private Long serverId;
    private String name;
    private Integer port;
    private ServerStatus status;
    private LocalDateTime lastReceivedAt;
    private JvmSummary jvm;
    private HttpSummary http;
    private HikariCpSummary hikaricp;
    private ExecutorsSummary executors;

    @Getter
    @Builder
    public static class JvmSummary {
        private Long heapUsedMB;
        private Long heapUsedMaxMB;
        private Double avgGcPauseMs;
    }

    @Getter
    @Builder
    public static class HttpSummary {
        private Double rps;
        private Double avgResTimeMs;
        private Double errorRatePct;
    }

    @Getter
    @Builder
    public static class HikariCpSummary {
        private Integer active;
        private Integer activeMax;
        private Integer pending;
    }

    @Getter
    @Builder
    public static class ExecutorsSummary {
        private Integer active;
        private Integer max;
        private Integer queuedTasks;
    }

    public static ServerSummaryResponse from(Server server, StatJvm jvm, StatHttp http,
            StatHikariCp hikari, StatThreadPool pool) {
        return ServerSummaryResponse.builder()
                .serverId(server.getId())
                .name(server.getName())
                .port(server.getPort())
                .status(server.getStatus())
                .lastReceivedAt(server.getLastReceivedAt())
                .jvm(createJvmSummary(jvm))
                .http(createHttpSummary(http))
                .hikaricp(createHikariSummary(hikari))
                .executors(createExecutorsSummary(pool))
                .build();
    }

    private static JvmSummary createJvmSummary(StatJvm jvm) {
        if (jvm == null) {
            return JvmSummary.builder().build();
        }

        Long heapUsedMB = jvm.getHeapUsedAvg() != null ? jvm.getHeapUsedAvg() / (1024 * 1024) : null;
        Long heapUsedMaxMB = jvm.getHeapUsedMax() != null ? jvm.getHeapUsedMax() / (1024 * 1024) : null;
        Double avgGcPauseMs = (jvm.getGcPauseSecondsSum() != null && jvm.getGcPauseCountSum() != null
                && jvm.getGcPauseCountSum() > 0)
                        ? (jvm.getGcPauseSecondsSum() / jvm.getGcPauseCountSum()) * 1000.0
                        : null;

        return JvmSummary.builder()
                .heapUsedMB(heapUsedMB)
                .heapUsedMaxMB(heapUsedMaxMB)
                .avgGcPauseMs(avgGcPauseMs)
                .build();
    }

    private static HttpSummary createHttpSummary(StatHttp http) {
        if (http == null) {
            return HttpSummary.builder().build();
        }

        return HttpSummary.builder()
                .rps(http.getRpsAvg())
                .avgResTimeMs(http.getAvgResTimeMs())
                .errorRatePct(http.getErrorRateAvg())
                .build();
    }

    private static HikariCpSummary createHikariSummary(StatHikariCp hikari) {
        if (hikari == null) {
            return HikariCpSummary.builder().build();
        }

        Integer active = hikari.getActivePoolAvg() != null ? hikari.getActivePoolAvg().intValue() : null;
        return HikariCpSummary.builder()
                .active(active)
                .activeMax(hikari.getActivePoolMax())
                .pending(hikari.getPendingThreadsMax())
                .build();
    }

    private static ExecutorsSummary createExecutorsSummary(StatThreadPool pool) {
        if (pool == null) {
            return ExecutorsSummary.builder().build();
        }

        Integer active = pool.getActiveThreadsAvg() != null ? pool.getActiveThreadsAvg().intValue() : null;
        Integer max = pool.getMaxThreadsAvg() != null ? pool.getMaxThreadsAvg().intValue() : null;
        return ExecutorsSummary.builder()
                .active(active)
                .max(max)
                .queuedTasks(pool.getQueuedTasksMax())
                .build();
    }
}
