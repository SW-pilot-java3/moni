package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.response.ServerRealtimeCurrentDto;
import com.moni.api.domain.server.dto.response.ServerRealtimeMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerRealtimeSeriesDto;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerMetricService {

    private final ServerRepository serverRepository;
    private final ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    public ServerRealtimeMetricsResponse getServerRealtimeMetrics(Long serverId, int limit) {
        if (!serverRepository.existsById(serverId)) {
            throw new CustomException(ServerErrorCode.SERVER_NOT_FOUND);
        }

        List<ServerRealtimeMetric> metrics = serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(
                serverId, PageRequest.of(0, limit));

        if (metrics.isEmpty()) {
            return ServerRealtimeMetricsResponse.of(null, Collections.emptyList());
        }

        ServerRealtimeMetric latest = metrics.get(0);
        ServerRealtimeCurrentDto current = ServerRealtimeCurrentDto.from(latest);

        List<ServerRealtimeSeriesDto> series = metrics.stream()
                .sorted(Comparator.comparing(ServerRealtimeMetric::getCollectedAt))
                .map(ServerRealtimeSeriesDto::from)
                .collect(Collectors.toList());

        return ServerRealtimeMetricsResponse.of(current, series);
    }
}
