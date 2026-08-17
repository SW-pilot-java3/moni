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
@Table(name = "server_executor_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerExecutorMetric {

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

    @Column(nullable = false, length = 100)
    private String name;

    private Integer active;

    private Integer max;

    @Column(name = "queued_tasks")
    private Integer queuedTasks;

    @Column(name = "queue_remaining")
    private Integer queueRemaining;

    @Builder
    public ServerExecutorMetric(ServerRealtimeMetric realtimeMetric, Long serverId, LocalDateTime collectedAt,
                                String name, Integer active, Integer max,
                                Integer queuedTasks, Integer queueRemaining) {
        this.realtimeMetric = realtimeMetric;
        this.serverId = serverId;
        this.collectedAt = collectedAt;
        this.name = name;
        this.active = active;
        this.max = max;
        this.queuedTasks = queuedTasks;
        this.queueRemaining = queueRemaining;
    }
}
