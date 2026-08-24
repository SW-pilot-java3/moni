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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "server_realtime_metrics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_server_realtime_metrics_server_collected",
                columnNames = {"server_id", "collected_at"})
)
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
    private Set<ServerHttpEndpointMetric> httpEndpoints = new LinkedHashSet<>();

    @OneToMany(mappedBy = "realtimeMetric", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ServerHikariCpPoolMetric> hikaricpPools = new LinkedHashSet<>();

    @OneToMany(mappedBy = "realtimeMetric", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ServerExecutorMetric> executors = new LinkedHashSet<>();

    @Builder
    public ServerRealtimeMetric(Long id, Long serverId, LocalDateTime collectedAt, JvmMetric jvmMetric,
            Collection<ServerHttpEndpointMetric> httpEndpoints,
            Collection<ServerHikariCpPoolMetric> hikaricpPools,
            Collection<ServerExecutorMetric> executors) {
        this.id = id;
        this.serverId = serverId;
        this.collectedAt = collectedAt;
        this.jvmMetric = jvmMetric;
        if (httpEndpoints != null) {
            this.httpEndpoints = new LinkedHashSet<>(httpEndpoints);
        }
        if (hikaricpPools != null) {
            this.hikaricpPools = new LinkedHashSet<>(hikaricpPools);
        }
        if (executors != null) {
            this.executors = new LinkedHashSet<>(executors);
        }
    }
}
