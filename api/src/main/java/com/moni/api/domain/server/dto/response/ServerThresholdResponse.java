package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerThresholdResponse {

    private ServerMetricKey metricKey;
    private Double warningValue;
    private Double criticalValue;
    private Boolean isCustomized;

    public static ServerThresholdResponse from(ServerThreshold threshold) {
        boolean isCustomized = !threshold.getWarningValue().equals(threshold.getMetricKey().getDefaultWarningValue())
                || !threshold.getCriticalValue().equals(threshold.getMetricKey().getDefaultCriticalValue());

        return ServerThresholdResponse.builder()
                .metricKey(threshold.getMetricKey())
                .warningValue(threshold.getWarningValue())
                .criticalValue(threshold.getCriticalValue())
                .isCustomized(isCustomized)
                .build();
    }
}
