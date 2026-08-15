package com.moni.api.domain.server.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServerMetricKey {

    JVM_HEAP_USAGE(80.0, 90.0, "JVM Heap 사용률 (%)"),
    JVM_OLD_GEN_USAGE(75.0, 85.0, "JVM Old Gen 메모리 사용률 (%)"),
    GC_PAUSE_TIME(0.5, 1.0, "GC 일시정지시간 (초)"),
    HTTP_AVG_LATENCY(500.0, 1000.0, "HTTP 평균 응답지연시간 (ms)"),
    HTTP_ERROR_RATE(1.0, 5.0, "HTTP 에러율 (%)"),
    HIKARICP_POOL_USAGE(80.0, 90.0, "DB 커넥션 풀 사용률 (%)"),
    THREADPOOL_QUEUE_USAGE(50.0, 80.0, "스레드풀 큐 적체율 (%)");

    private final Double defaultWarningValue;
    private final Double defaultCriticalValue;
    private final String description;
}
