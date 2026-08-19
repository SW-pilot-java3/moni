package com.moni.api.domain.instance.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryMetrics {

    @Column(name = "mem_total_bytes")
    private Long memTotalBytes;

    @Column(name = "mem_free_bytes")
    private Long memFreeBytes;

    @Column(name = "mem_available_bytes")
    private Long memAvailableBytes;

    @Column(name = "buffers_bytes")
    private Long buffersBytes;

    @Column(name = "cached_bytes")
    private Long cachedBytes;

    @Column(name = "swap_total_bytes")
    private Long swapTotalBytes;

    @Column(name = "swap_free_bytes")
    private Long swapFreeBytes;

    @Builder
    public MemoryMetrics(Long memTotalBytes, Long memFreeBytes, Long memAvailableBytes,
                          Long buffersBytes, Long cachedBytes, Long swapTotalBytes, Long swapFreeBytes) {
        this.memTotalBytes = memTotalBytes;
        this.memFreeBytes = memFreeBytes;
        this.memAvailableBytes = memAvailableBytes;
        this.buffersBytes = buffersBytes;
        this.cachedBytes = cachedBytes;
        this.swapTotalBytes = swapTotalBytes;
        this.swapFreeBytes = swapFreeBytes;
    }
}