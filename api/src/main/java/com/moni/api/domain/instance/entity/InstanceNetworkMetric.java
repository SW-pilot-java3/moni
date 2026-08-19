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
@Entity(name = "instance_network_metrics")
@Table(
        name = "instance_network_metrics",
        indexes = @Index(name = "idx_instance_network_metrics_realtime_collected", columnList = "realtime_metric_id, collected_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_network_metrics_metric_interface",
                columnNames = {"realtime_metric_id", "interface_name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceNetworkMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_metric_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InstanceRealtimeMetric realtimeMetric;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "interface_name", length = 50, nullable = false)
    private String interfaceName;

    @Column(name = "rx_bytes_total")
    private Long rxBytesTotal;

    @Column(name = "tx_bytes_total")
    private Long txBytesTotal;

    @Column(name = "rx_errors_total")
    private Long rxErrorsTotal;

    @Column(name = "tx_errors_total")
    private Long txErrorsTotal;

    @Builder
    public InstanceNetworkMetric(InstanceRealtimeMetric realtimeMetric, LocalDateTime collectedAt,
                                 String interfaceName, Long rxBytesTotal, Long txBytesTotal,
                                 Long rxErrorsTotal, Long txErrorsTotal) {
        this.realtimeMetric = realtimeMetric;
        this.collectedAt = collectedAt;
        this.interfaceName = interfaceName;
        this.rxBytesTotal = rxBytesTotal;
        this.txBytesTotal = txBytesTotal;
        this.rxErrorsTotal = rxErrorsTotal;
        this.txErrorsTotal = txErrorsTotal;
    }
}