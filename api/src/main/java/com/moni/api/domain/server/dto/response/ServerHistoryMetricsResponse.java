package com.moni.api.domain.server.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerHistoryMetricsResponse {

    private Long serverId;
    private LocalDate date;
    private ServerHistorySummaryDto summary;
    private List<ServerHistorySeriesDto> series;

    public static ServerHistoryMetricsResponse of(
            Long serverId,
            LocalDate date,
            ServerHistorySummaryDto summary,
            List<ServerHistorySeriesDto> series) {
        return ServerHistoryMetricsResponse.builder()
                .serverId(serverId)
                .date(date)
                .summary(summary)
                .series(series)
                .build();
    }
}
