package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerRealtimeSeriesDto {

    private LocalDateTime collectedAt;
    private Long jvmHeapUsedBytes;
    private Long jvmHeapMaxBytes;
    private Long jvmOldGenUsedBytes;
    private Double gcPauseSecondsSum;
    private Double totalRps;
    private Double avgLatencyMs;
    private Integer hikaricpActiveTotal;
    private Integer executorActiveTotal;

    public static ServerRealtimeSeriesDto from(ServerRealtimeMetric metric) {
        JvmMetric jvm = metric.getJvmMetric();

        Long heapUsed = jvm != null ? jvm.getJvmHeapUsedBytes() : 0L;
        Long heapMax = jvm != null ? jvm.getJvmHeapMaxBytes() : 0L;
        Long oldGenUsed = jvm != null ? jvm.getJvmOldGenUsedBytes() : 0L;
        Double gcPauseSum = jvm != null && jvm.getGcPauseSecondsSum() != null ? jvm.getGcPauseSecondsSum() : 0.0;

        double totalSum = 0.0;
        long totalCount = 0L;
        if (metric.getHttpEndpoints() != null) {
            for (ServerHttpEndpointMetric ep : metric.getHttpEndpoints()) {
                if (ep.getRequestsSum() != null) {
                    totalSum += ep.getRequestsSum();
                }
                if (ep.getRequestsCount() != null) {
                    totalCount += ep.getRequestsCount();
                }
            }
        }
        double avgLatency = totalCount > 0 ? (totalSum / totalCount) * 1000.0 : 0.0;

        int activeHikari = 0;
        if (metric.getHikaricpPools() != null) {
            for (ServerHikariCpPoolMetric pool : metric.getHikaricpPools()) {
                if (pool.getActive() != null) {
                    activeHikari += pool.getActive();
                }
            }
        }

        int activeExecutors = 0;
        if (metric.getExecutors() != null) {
            for (ServerExecutorMetric exec : metric.getExecutors()) {
                if (exec.getActive() != null) {
                    activeExecutors += exec.getActive();
                }
            }
        }

        return ServerRealtimeSeriesDto.builder()
                .collectedAt(metric.getCollectedAt())
                .jvmHeapUsedBytes(heapUsed)
                .jvmHeapMaxBytes(heapMax)
                .jvmOldGenUsedBytes(oldGenUsed)
                .gcPauseSecondsSum(gcPauseSum)
                .totalRps(0.0)
                .avgLatencyMs(Math.round(avgLatency * 10.0) / 10.0)
                .hikaricpActiveTotal(activeHikari)
                .executorActiveTotal(activeExecutors)
                .build();
    }
}
