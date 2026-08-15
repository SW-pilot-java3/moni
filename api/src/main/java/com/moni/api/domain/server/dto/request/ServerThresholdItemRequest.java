package com.moni.api.domain.server.dto.request;

import com.moni.api.domain.server.entity.ServerMetricKey;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServerThresholdItemRequest {

    @NotNull(message = "metricKey는 필수입니다.")
    private ServerMetricKey metricKey;

    @NotNull(message = "warningValue는 필수입니다.")
    private Double warningValue;

    @NotNull(message = "criticalValue는 필수입니다.")
    private Double criticalValue;
}
