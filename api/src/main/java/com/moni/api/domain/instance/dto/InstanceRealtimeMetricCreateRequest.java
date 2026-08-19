package com.moni.api.domain.instance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record InstanceRealtimeMetricCreateRequest(
        @NotNull LocalDateTime collectedAt,
        @NotNull @Valid InstancePayload instance
) {
    public record InstancePayload(
            Double cpuSecondsTotal,
            Double cpuIdleSecondsTotal,
            Double cpuIOWaitSecondsTotal,
            Long memTotalBytes,
            Long memFreeBytes,
            Long memAvailableBytes,
            Long buffersBytes,
            Long cachedBytes,
            Long swapTotalBytes,
            Long swapFreeBytes,
            @NotEmpty @Valid List<CoreCpu> cpus,
            @NotEmpty @Valid List<DiskDevice> disks,
            @NotEmpty @Valid List<FileSystemMount> filesystems,
            @NotEmpty @Valid List<NetworkInterfaceMetric> networks
    ) {
    }

    public record CoreCpu(
            @NotNull Integer coreId,
            Double cpuSecondsTotal,
            Double cpuIdleSecondsTotal,
            Double cpuIowaitSecondsTotal
    ) {
    }

    public record DiskDevice(
            @NotNull String deviceName,
            Long readsTotal,
            Long writesTotal,
            Long readBytesTotal,
            Long writtenBytesTotal,
            Double ioTimeSecondsTotal
    ) {
    }

    public record FileSystemMount(
            @NotNull String mountPoint,
            Long fsSizeBytes,
            Long fsAvailBytes
    ) {
    }

    public record NetworkInterfaceMetric(
            @NotNull String interfaceName,
            Long rxBytesTotal,
            Long txBytesTotal,
            Long rxErrorsTotal,
            Long txErrorsTotal
    ) {
    }
}
