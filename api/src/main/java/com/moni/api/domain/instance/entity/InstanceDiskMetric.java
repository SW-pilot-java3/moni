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
@Entity(name = "instance_disk_metrics")
@Table(
        name = "instance_disk_metrics",
        indexes = @Index(name = "idx_instance_disk_metrics_realtime_collected", columnList = "realtime_metric_id, collected_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_disk_metrics_metric_device",
                columnNames = {"realtime_metric_id", "device_name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceDiskMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_metric_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InstanceRealtimeMetric realtimeMetric;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "device_name", length = 50, nullable = false)
    private String deviceName;

    @Column(name = "reads_total")
    private Long readsTotal;

    @Column(name = "writes_total")
    private Long writesTotal;

    @Column(name = "read_bytes_total")
    private Long readBytesTotal;

    @Column(name = "written_bytes_total")
    private Long writtenBytesTotal;

    @Column(name = "io_time_seconds_total")
    private Double ioTimeSecondsTotal;

    @Builder
    public InstanceDiskMetric(InstanceRealtimeMetric realtimeMetric, LocalDateTime collectedAt,
                              String deviceName, Long readsTotal, Long writesTotal,
                              Long readBytesTotal, Long writtenBytesTotal, Double ioTimeSecondsTotal) {
        this.realtimeMetric = realtimeMetric;
        this.collectedAt = collectedAt;
        this.deviceName = deviceName;
        this.readsTotal = readsTotal;
        this.writesTotal = writesTotal;
        this.readBytesTotal = readBytesTotal;
        this.writtenBytesTotal = writtenBytesTotal;
        this.ioTimeSecondsTotal = ioTimeSecondsTotal;
    }
}