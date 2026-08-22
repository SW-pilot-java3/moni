package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.request.ServerThresholdItemRequest;
import com.moni.api.domain.server.dto.request.ServerThresholdsPatchRequest;
import com.moni.api.domain.server.dto.response.ServerThresholdResponse;
import com.moni.api.domain.server.dto.response.ServerThresholdUpdateResponse;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.domain.server.validator.ServerValidator;
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

    private final ServerThresholdRepository serverThresholdRepository;
    private final ServerValidator serverValidator;

    public List<ServerThresholdResponse> getThresholds(Long serverId, Long userId) {
        serverValidator.validateAndGetServer(serverId, userId);

        List<ServerThreshold> thresholds = serverThresholdRepository.findByServerId(serverId);
        return thresholds.stream()
                .map(ServerThresholdResponse::from)
                .toList();
    }

    @Transactional
    public ServerThresholdUpdateResponse updateThresholds(Long serverId, Long userId, ServerThresholdsPatchRequest request) {
        serverValidator.validateAndGetServer(serverId, userId);

        int updatedCount = request.getThresholds().size();

        for (ServerThresholdItemRequest item : request.getThresholds()) {
            if (item.getWarningValue() >= item.getCriticalValue()) {
                throw new CustomException(ServerErrorCode.INVALID_THRESHOLD_VALUE);
            }

            ServerThreshold threshold = serverThresholdRepository
                    .findByServerIdAndMetricKey(serverId, item.getMetricKey())
                    .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_THRESHOLD_NOT_FOUND));

            threshold.updateThreshold(item.getWarningValue(), item.getCriticalValue());
        }

        return ServerThresholdUpdateResponse.of(serverId, updatedCount, LocalDateTime.now());
    }
}
