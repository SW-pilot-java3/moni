package com.moni.api.domain.instance.dto;

public record InstanceHistorySummaryDto(
        CpuSummary cpu,
        MemorySummary memory,
        DiskSummary disk,
        NetworkSummary network
) {

    public record CpuSummary(
            Double cpuUsageAvg,
            Double cpuUsageMax,
            Double cpuIowaitAvg
    ) {
    }

    public record MemorySummary(
            Long memAvailableAvg,
            Long memAvailableMin,
            Double swapUsedMax
    ) {
    }

    public record DiskSummary(
            Double readIopsAvg,
            Double writeIopsAvg,
            Double diskUtilMax,
            Double diskUsedPctMax
    ) {
    }

    public record NetworkSummary(
            Double rxMbpsAvg,
            Double txMbpsAvg,
            Integer errorsSum
    ) {
    }
}
