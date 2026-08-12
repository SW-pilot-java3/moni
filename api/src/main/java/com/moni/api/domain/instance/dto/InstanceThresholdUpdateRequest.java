package com.moni.api.domain.instance.dto;

import com.moni.api.domain.instance.enums.MetricKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InstanceThresholdUpdateRequest(
        @NotEmpty @Valid List<ThresholdItem> thresholds
) {
    public record ThresholdItem(
            @NotNull MetricKey metricKey,
            @NotNull Double warningValue,
            @NotNull Double criticalValue
    ) {
    }
}