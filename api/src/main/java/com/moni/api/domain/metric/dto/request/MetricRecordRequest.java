package com.moni.api.domain.metric.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MetricRecordRequest {

    @NotNull(message = "수집 일시(collectedAt)는 필수입니다.")
    private Instant collectedAt;

    @Valid
    private InstanceMetricPayload instance;

    @Valid
    private ServerMetricPayload server;

    @Builder
    public MetricRecordRequest(Instant collectedAt, ServerMetricPayload server, InstanceMetricPayload instance) {
        this.collectedAt = collectedAt;
        this.server = server;
        this.instance = instance;
    }

    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.ofInstant(collectedAt, ZoneId.systemDefault());
    }

    @Getter
    @NoArgsConstructor
    public static class InstanceMetricPayload {
        private Double cpuSecondsTotal;
        private Double cpuIdleSecondsTotal;

        @JsonAlias({"cpuIOWaitSecondsTotal", "cpuIOwaitSecondsTotal", "cpuIowaitSecondsTotal"})
        private Double cpuIowaitSecondsTotal;

        private Long memTotalBytes;
        private Long memFreeBytes;
        private Long memAvailableBytes;
        private Long buffersBytes;
        private Long cachedBytes;
        private Long swapTotalBytes;
        private Long swapFreeBytes;

        private List<InstanceCpuPayload> cpus;
        private List<InstanceDiskPayload> disks;
        private List<InstanceFilesystemPayload> filesystems;
        private List<InstanceNetworkPayload> networks;

        @Builder
        public InstanceMetricPayload(Double cpuSecondsTotal, Double cpuIdleSecondsTotal, Double cpuIowaitSecondsTotal,
                                     Long memTotalBytes, Long memFreeBytes, Long memAvailableBytes,
                                     Long buffersBytes, Long cachedBytes, Long swapTotalBytes, Long swapFreeBytes,
                                     List<InstanceCpuPayload> cpus, List<InstanceDiskPayload> disks,
                                     List<InstanceFilesystemPayload> filesystems, List<InstanceNetworkPayload> networks) {
            this.cpuSecondsTotal = cpuSecondsTotal;
            this.cpuIdleSecondsTotal = cpuIdleSecondsTotal;
            this.cpuIowaitSecondsTotal = cpuIowaitSecondsTotal;
            this.memTotalBytes = memTotalBytes;
            this.memFreeBytes = memFreeBytes;
            this.memAvailableBytes = memAvailableBytes;
            this.buffersBytes = buffersBytes;
            this.cachedBytes = cachedBytes;
            this.swapTotalBytes = swapTotalBytes;
            this.swapFreeBytes = swapFreeBytes;
            this.cpus = cpus;
            this.disks = disks;
            this.filesystems = filesystems;
            this.networks = networks;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class InstanceCpuPayload {
        private Integer coreId;
        private Double cpuSecondsTotal;
        private Double cpuIdleSecondsTotal;
        private Double cpuIowaitSecondsTotal;

        @Builder
        public InstanceCpuPayload(Integer coreId, Double cpuSecondsTotal, Double cpuIdleSecondsTotal, Double cpuIowaitSecondsTotal) {
            this.coreId = coreId;
            this.cpuSecondsTotal = cpuSecondsTotal;
            this.cpuIdleSecondsTotal = cpuIdleSecondsTotal;
            this.cpuIowaitSecondsTotal = cpuIowaitSecondsTotal;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class InstanceDiskPayload {
        private String deviceName;
        private Long readsTotal;
        private Long writesTotal;
        private Long readBytesTotal;
        private Long writtenBytesTotal;
        private Double ioTimeSecondsTotal;

        @Builder
        public InstanceDiskPayload(String deviceName, Long readsTotal, Long writesTotal,
                                   Long readBytesTotal, Long writtenBytesTotal, Double ioTimeSecondsTotal) {
            this.deviceName = deviceName;
            this.readsTotal = readsTotal;
            this.writesTotal = writesTotal;
            this.readBytesTotal = readBytesTotal;
            this.writtenBytesTotal = writtenBytesTotal;
            this.ioTimeSecondsTotal = ioTimeSecondsTotal;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class InstanceFilesystemPayload {
        private String mountPoint;
        private Long fsSizeBytes;
        private Long fsAvailBytes;

        @Builder
        public InstanceFilesystemPayload(String mountPoint, Long fsSizeBytes, Long fsAvailBytes) {
            this.mountPoint = mountPoint;
            this.fsSizeBytes = fsSizeBytes;
            this.fsAvailBytes = fsAvailBytes;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class InstanceNetworkPayload {
        private String interfaceName;
        private Long rxBytesTotal;
        private Long txBytesTotal;
        private Long rxErrorsTotal;
        private Long txErrorsTotal;

        @Builder
        public InstanceNetworkPayload(String interfaceName, Long rxBytesTotal, Long txBytesTotal,
                                      Long rxErrorsTotal, Long txErrorsTotal) {
            this.interfaceName = interfaceName;
            this.rxBytesTotal = rxBytesTotal;
            this.txBytesTotal = txBytesTotal;
            this.rxErrorsTotal = rxErrorsTotal;
            this.txErrorsTotal = txErrorsTotal;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ServerMetricPayload {
        private Long jvmHeapUsedBytes;
        private Long jvmHeapMaxBytes;
        private Long jvmOldGenUsedBytes;
        private Long jvmOldGenMaxBytes;
        private Long gcPauseSecondsCount;
        private Double gcPauseSecondsSum;
        private Double processUptimeSeconds;
        private Integer jvmThreadsLive;
        private Integer jvmThreadsBlocked;
        private List<HttpEndpointPayload> httpEndpoints;
        private List<HikariPoolPayload> hikaricpPools;
        private List<ExecutorPayload> executors;

        @Builder
        public ServerMetricPayload(Long jvmHeapUsedBytes, Long jvmHeapMaxBytes, Long jvmOldGenUsedBytes,
                                   Long jvmOldGenMaxBytes, Long gcPauseSecondsCount, Double gcPauseSecondsSum,
                                   Double processUptimeSeconds, Integer jvmThreadsLive, Integer jvmThreadsBlocked,
                                   List<HttpEndpointPayload> httpEndpoints,
                                   List<HikariPoolPayload> hikaricpPools,
                                   List<ExecutorPayload> executors) {
            this.jvmHeapUsedBytes = jvmHeapUsedBytes;
            this.jvmHeapMaxBytes = jvmHeapMaxBytes;
            this.jvmOldGenUsedBytes = jvmOldGenUsedBytes;
            this.jvmOldGenMaxBytes = jvmOldGenMaxBytes;
            this.gcPauseSecondsCount = gcPauseSecondsCount;
            this.gcPauseSecondsSum = gcPauseSecondsSum;
            this.processUptimeSeconds = processUptimeSeconds;
            this.jvmThreadsLive = jvmThreadsLive;
            this.jvmThreadsBlocked = jvmThreadsBlocked;
            this.httpEndpoints = httpEndpoints;
            this.hikaricpPools = hikaricpPools;
            this.executors = executors;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class HttpEndpointPayload {
        private String uri;
        private String method;
        private String status;
        private Long requestsCount;
        private Double requestsSum;
        private Double requestsMax;

        @Builder
        public HttpEndpointPayload(String uri, String method, String status,
                                   Long requestsCount, Double requestsSum, Double requestsMax) {
            this.uri = uri;
            this.method = method;
            this.status = status;
            this.requestsCount = requestsCount;
            this.requestsSum = requestsSum;
            this.requestsMax = requestsMax;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class HikariPoolPayload {
        private String poolName;
        private Integer active;
        private Integer idle;
        private Integer pending;
        private Integer max;
        private Long timeoutsTotal;

        @Builder
        public HikariPoolPayload(String poolName, Integer active, Integer idle,
                                 Integer pending, Integer max, Long timeoutsTotal) {
            this.poolName = poolName;
            this.active = active;
            this.idle = idle;
            this.pending = pending;
            this.max = max;
            this.timeoutsTotal = timeoutsTotal;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ExecutorPayload {
        private String name;
        private Integer active;
        private Integer max;
        private Integer queuedTasks;
        private Integer queueRemaining;

        @Builder
        public ExecutorPayload(String name, Integer active, Integer max,
                               Integer queuedTasks, Integer queueRemaining) {
            this.name = name;
            this.active = active;
            this.max = max;
            this.queuedTasks = queuedTasks;
            this.queueRemaining = queueRemaining;
        }
    }
}
