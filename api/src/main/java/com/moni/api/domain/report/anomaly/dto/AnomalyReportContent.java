package com.moni.api.domain.report.anomaly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnomalyReportContent(
        String summary,
        @JsonProperty("probable_cause") String probableCause,
        String recommendation
) {
}