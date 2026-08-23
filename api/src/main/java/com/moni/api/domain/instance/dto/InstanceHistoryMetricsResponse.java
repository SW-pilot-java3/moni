package com.moni.api.domain.instance.dto;

import java.time.LocalDate;
import java.util.List;

public record InstanceHistoryMetricsResponse(
        Long instanceId,
        LocalDate date,
        InstanceHistorySummaryDto summary,
        List<InstanceHistorySeriesDto> series
) {

    public static InstanceHistoryMetricsResponse of(
            Long instanceId, LocalDate date, InstanceHistorySummaryDto summary, List<InstanceHistorySeriesDto> series) {
        return new InstanceHistoryMetricsResponse(instanceId, date, summary, series);
    }
}
