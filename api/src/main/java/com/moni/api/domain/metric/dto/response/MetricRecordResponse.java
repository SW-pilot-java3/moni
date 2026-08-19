package com.moni.api.domain.metric.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MetricRecordResponse {

    private LocalDateTime receivedAt;
    private String status;

    @Builder
    public MetricRecordResponse(LocalDateTime receivedAt, String status) {
        this.receivedAt = receivedAt;
        this.status = status;
    }

    public static MetricRecordResponse of(LocalDateTime receivedAt, String status) {
        return MetricRecordResponse.builder()
                .receivedAt(receivedAt)
                .status(status)
                .build();
    }
}
