package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.request.ServerThresholdItemRequest;
import com.moni.api.domain.server.dto.request.ServerThresholdsPatchRequest;
import com.moni.api.domain.server.dto.response.ServerThresholdUpdateResponse;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerThresholdService {

    private final ServerRepository serverRepository;
    private final ServerThresholdRepository serverThresholdRepository;

    @Transactional
    public ServerThresholdUpdateResponse updateThresholds(Long serverId, ServerThresholdsPatchRequest request) {
        if (!serverRepository.existsById(serverId)) {
            throw new CustomException(ServerErrorCode.SERVER_NOT_FOUND);
        }

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
