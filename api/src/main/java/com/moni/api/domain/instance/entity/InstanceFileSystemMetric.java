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
@Entity(name = "instance_filesystem_metrics")
@Table(
        name = "instance_filesystem_metrics",
        indexes = @Index(name = "idx_instance_filesystem_metrics_realtime_collected", columnList = "realtime_metric_id, collected_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_filesystem_metrics_metric_mount",
                columnNames = {"realtime_metric_id", "mount_point"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceFileSystemMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_metric_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InstanceRealtimeMetric realtimeMetric;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "mount_point", nullable = false, length = 255)
    private String mountPoint;

    @Column(name = "fs_size_bytes")
    private Long fsSizeBytes;

    @Column(name = "fs_avail_bytes")
    private Long fsAvailBytes;

    @Builder
    public InstanceFileSystemMetric(InstanceRealtimeMetric realtimeMetric, LocalDateTime collectedAt,
                                     String mountPoint, Long fsSizeBytes, Long fsAvailBytes) {
        this.realtimeMetric = realtimeMetric;
        this.collectedAt = collectedAt;
        this.mountPoint = mountPoint;
        this.fsSizeBytes = fsSizeBytes;
        this.fsAvailBytes = fsAvailBytes;
    }
}
