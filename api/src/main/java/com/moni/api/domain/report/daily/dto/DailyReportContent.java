package com.moni.api.domain.report.daily.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DailyReportContent(
        String summary,
        @JsonProperty("trend_analysis") String trendAnalysis,
        @JsonProperty("notable_events") List<String> notableEvents,
        String recommendation
) {
}
