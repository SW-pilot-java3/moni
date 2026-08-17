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
public class ServerRealtimeMetricsResponse {

    private ServerRealtimeCurrentDto current;
    private List<ServerRealtimeSeriesDto> series;

    public static ServerRealtimeMetricsResponse of(ServerRealtimeCurrentDto current, List<ServerRealtimeSeriesDto> series) {
        return ServerRealtimeMetricsResponse.builder()
                .current(current)
                .series(series)
                .build();
    }
}
