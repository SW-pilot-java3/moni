package com.moni.api.domain.server.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.server.dto.response.ServerSseStreamResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.mapper.ServerRealtimeMetricMapper;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerRealtimeMetricService {

    private final ServerRealtimeMetricRepository serverRealtimeMetricRepository;
    private final ServerSseService serverSseService;

    private final Cache<String, EndpointSnapshot> endpointSnapshots = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(100_000)
            .build();

    private record EndpointSnapshot(long requestsCount, LocalDateTime collectedAt) {
    }

    @Transactional
    public void recordServerMetric(Server server, LocalDateTime collectedAt, MetricRecordRequest.ServerMetricPayload payload) {
        if (server == null || payload == null) {
            return;
        }

        // 서버 실시간 원시 메트릭 DB 저장
        ServerRealtimeMetric realtimeMetric = ServerRealtimeMetricMapper.toEntity(server.getId(), collectedAt, payload);
        serverRealtimeMetricRepository.save(realtimeMetric);

        // 서버 상태(CONNECTED) 및 마지막 수신 시각 갱신
        server.updateStatus(ServerStatus.CONNECTED);
        server.updateLastReceivedAt(collectedAt);

        // SSE 브로드캐스트
        ServerSseStreamResponse sseResponse = ServerRealtimeMetricMapper.toSseResponse(
                server.getId(),
                collectedAt,
                payload,
                (uri, method, count) -> calculateEndpointRps(server.getId(), uri, method, count, collectedAt)
        );
        serverSseService.broadcastServerMetric(server.getId(), sseResponse);
    }

    private Double calculateEndpointRps(Long serverId, String uri, String method, long currentCount,
            LocalDateTime collectedAt) {
        String key = serverId + ":" + uri + ":" + method;
        EndpointSnapshot prev = endpointSnapshots.getIfPresent(key);
        endpointSnapshots.put(key, new EndpointSnapshot(currentCount, collectedAt));

        if (prev == null) {
            return 0.0;
        }

        long secondsDiff = Duration.between(prev.collectedAt(), collectedAt).getSeconds();
        if (secondsDiff <= 0) {
            return 0.0;
        }

        long countDiff = currentCount - prev.requestsCount();
        if (countDiff < 0) {
            countDiff = currentCount;
        }

        double rps = (double) countDiff / secondsDiff;
        return Math.round(rps * 10.0) / 10.0;
    }
}
