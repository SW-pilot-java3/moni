package com.moni.api.domain.metric.service;

import com.moni.api.domain.instance.mapper.InstanceRealtimeMetricMapper;
import com.moni.api.domain.instance.service.InstanceRealtimeMetricService;
import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.metric.dto.response.MetricRecordResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.service.ServerApiKeyService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private final ServerApiKeyService serverApiKeyService;
    private final InstanceRealtimeMetricService instanceRealtimeMetricService;

    @Transactional
    public MetricRecordResponse recordMetrics(String rawApiKey, MetricRecordRequest request) {
        // API Key 검증 및 대상 서버 식별
        Server server = serverApiKeyService.authenticate(rawApiKey);

        instanceRealtimeMetricService.recordMetric(
                server.getInstance().getId(), InstanceRealtimeMetricMapper.from(request));
        // =========================================================================
        // TODO: [Server Domain] 서버 메트릭 저장, 서버 상태(CONNECTED/lastReceivedAt) 갱신 및 Server SSE 실시간 브로드캐스트
        // =========================================================================

        return MetricRecordResponse.of(LocalDateTime.now(), "ACCEPTED");
    }
}
