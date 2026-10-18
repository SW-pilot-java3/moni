package com.moni.api.domain.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class JvmMetric {

    @Column(name = "jvm_heap_used_bytes")
    private Long jvmHeapUsedBytes;

    @Column(name = "jvm_heap_max_bytes")
    private Long jvmHeapMaxBytes;

    @Column(name = "jvm_old_gen_used_bytes")
    private Long jvmOldGenUsedBytes;

    @Column(name = "jvm_old_gen_max_bytes")
    private Long jvmOldGenMaxBytes;

    @Column(name = "gc_pause_seconds_count")
    private Long gcPauseSecondsCount;

    @Column(name = "gc_pause_seconds_sum")
    private Double gcPauseSecondsSum;

    @Column(name = "process_uptime_seconds")
    private Double processUptimeSeconds;

    @Column(name = "jvm_threads_live")
    private Integer jvmThreadsLive;

    @Column(name = "jvm_threads_blocked")
    private Integer jvmThreadsBlocked;
}
