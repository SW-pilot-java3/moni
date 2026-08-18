package com.moni.api.domain.instance.entity;

import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;
import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "instance_realtime_metrics",
        indexes = @Index(name = "idx_instance_realtime_metrics_instance_collected", columnList = "instance_id, collected_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceRealtimeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Instance instance;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Embedded
    private CpuMetrics cpuMetrics;

    @Embedded
    private MemoryMetrics memoryMetrics;

    @Builder
    public InstanceRealtimeMetric(Instance instance, LocalDateTime collectedAt,
                                   CpuMetrics cpuMetrics, MemoryMetrics memoryMetrics) {
        this.instance = instance;
        this.collectedAt = collectedAt;
        this.cpuMetrics = cpuMetrics;
        this.memoryMetrics = memoryMetrics;
    }
}