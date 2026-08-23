package com.moni.api.domain.instance.dto;

import java.time.LocalDateTime;

public record InstanceHistorySeriesDto(
        LocalDateTime statTime,
        Double cpuUsageAvg,
        Double cpuUsageMax,
        Long memAvailableAvg,
        Double readIopsAvg,
        Double writeIopsAvg,
        Double diskUsedPctMax,
        Double rxMbpsAvg,
        Double txMbpsAvg
) {
}
