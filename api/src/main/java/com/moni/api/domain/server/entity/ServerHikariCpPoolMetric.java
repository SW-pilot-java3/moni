package com.moni.api.domain.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server_hikaricp_pool_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerHikariCpPoolMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtime_metric_id")
    private ServerRealtimeMetric realtimeMetric;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "pool_name", nullable = false, length = 50)
    private String poolName;

    private Integer active;

    private Integer idle;

    private Integer pending;

    private Integer max;

    @Column(name = "timeouts_total")
    private Long timeoutsTotal;

    @Builder
    public ServerHikariCpPoolMetric(ServerRealtimeMetric realtimeMetric, Long serverId, LocalDateTime collectedAt,
                                   String poolName, Integer active, Integer idle, Integer pending,
                                   Integer max, Long timeoutsTotal) {
        this.realtimeMetric = realtimeMetric;
        this.serverId = serverId;
        this.collectedAt = collectedAt;
        this.poolName = poolName;
        this.active = active;
        this.idle = idle;
        this.pending = pending;
        this.max = max;
        this.timeoutsTotal = timeoutsTotal;
    }
}
