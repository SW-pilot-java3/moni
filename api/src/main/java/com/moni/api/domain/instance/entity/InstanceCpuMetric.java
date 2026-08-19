package com.moni.api.domain.instance.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Entity(name = "instance_cpu_metrics")
@Table(
        name = "instance_cpu_metrics",
        indexes = @Index(name = "idx_instance_cpu_metrics_realtime_collected", columnList = "realtime_metric_id, collected_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_cpu_metrics_metric_core",
                columnNames = {"realtime_metric_id", "core_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceCpuMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_metric_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InstanceRealtimeMetric realtimeMetric;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "core_id", nullable = false)
    private Integer coreId;

    @Column(name = "cpu_seconds_total")
    private Double cpuSecondsTotal;

    @Column(name = "cpu_idle_seconds_total")
    private Double cpuIdleSecondsTotal;

    @Column(name = "cpu_iowait_seconds_total")
    private Double cpuIowaitSecondsTotal;

    @Builder
    public InstanceCpuMetric(InstanceRealtimeMetric realtimeMetric, LocalDateTime collectedAt,
                              Integer coreId, Double cpuSecondsTotal, Double cpuIdleSecondsTotal,
                              Double cpuIowaitSecondsTotal) {
        this.realtimeMetric = realtimeMetric;
        this.collectedAt = collectedAt;
        this.coreId = coreId;
        this.cpuSecondsTotal = cpuSecondsTotal;
        this.cpuIdleSecondsTotal = cpuIdleSecondsTotal;
        this.cpuIowaitSecondsTotal = cpuIowaitSecondsTotal;
    }
}
