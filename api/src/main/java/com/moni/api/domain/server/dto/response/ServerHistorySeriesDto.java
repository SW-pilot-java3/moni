package com.moni.api.domain.server.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerHistorySeriesDto {

    private LocalDateTime statTime;
    private Long jvmHeapUsedBytes;
    private Long jvmHeapMaxBytes;
    private Long jvmOldGenUsedBytes;
    private Double gcPauseSecondsSum;
    private Double totalRpsAvg;
    private Double avgLatencyMs;
    private Double hikaricpActiveAvg;
    private Double executorActiveAvg;
}
