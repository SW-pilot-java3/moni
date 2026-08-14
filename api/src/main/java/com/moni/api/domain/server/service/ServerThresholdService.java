package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.request.ServerThresholdItemRequest;
import com.moni.api.domain.server.dto.request.ServerThresholdsPatchRequest;
import com.moni.api.domain.server.dto.response.ServerThresholdResponse;
import com.moni.api.domain.server.dto.response.ServerThresholdUpdateResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerThresholdService {

    private final ServerRepository serverRepository;
    private final ServerThresholdRepository serverThresholdRepository;

    public List<ServerThresholdResponse> getThresholds(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new CustomException(ServerErrorCode.SERVER_NOT_FOUND);
        }

        List<ServerThreshold> thresholds = serverThresholdRepository.findByServerId(serverId);
        return thresholds.stream()
                .map(ServerThresholdResponse::from)
                .toList();
    }

    @Transactional
    public ServerThresholdUpdateResponse updateThresholds(Long serverId, ServerThresholdsPatchRequest request) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        int updatedCount = request.getThresholds().size();

        for (ServerThresholdItemRequest item : request.getThresholds()) {
            if (item.getWarningValue() >= item.getCriticalValue()) {
                throw new CustomException(ServerErrorCode.INVALID_THRESHOLD_VALUE);
            }

            ServerThreshold threshold = serverThresholdRepository
                    .findByServerIdAndMetricKey(serverId, item.getMetricKey())
                    .orElseGet(() -> ServerThreshold.builder()
                            .server(server)
                            .metricKey(item.getMetricKey())
                            .warningValue(item.getWarningValue())
                            .criticalValue(item.getCriticalValue())
                            .build());

            if (threshold.getId() != null) {
                threshold.updateThreshold(item.getWarningValue(), item.getCriticalValue());
            } else {
                serverThresholdRepository.save(threshold);
            }
        }

        return ServerThresholdUpdateResponse.of(serverId, updatedCount, LocalDateTime.now());
    }
}
