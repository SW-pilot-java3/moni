package com.moni.api.domain.server.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server_realtime_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerRealtimeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Embedded
    private JvmMetric jvmMetric;

    @OneToMany(mappedBy = "realtimeMetric", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServerHttpEndpointMetric> httpEndpoints = new ArrayList<>();

    @OneToMany(mappedBy = "realtimeMetric", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServerHikariCpPoolMetric> hikaricpPools = new ArrayList<>();

    @OneToMany(mappedBy = "realtimeMetric", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServerExecutorMetric> executors = new ArrayList<>();

    @Builder
    public ServerRealtimeMetric(Long id, Long serverId, LocalDateTime collectedAt, JvmMetric jvmMetric,
            List<ServerHttpEndpointMetric> httpEndpoints,
            List<ServerHikariCpPoolMetric> hikaricpPools,
            List<ServerExecutorMetric> executors) {
        this.id = id;
        this.serverId = serverId;
        this.collectedAt = collectedAt;
        this.jvmMetric = jvmMetric;
        if (httpEndpoints != null) {
            this.httpEndpoints = httpEndpoints;
        }
        if (hikaricpPools != null) {
            this.hikaricpPools = hikaricpPools;
        }
        if (executors != null) {
            this.executors = executors;
        }
    }
}
