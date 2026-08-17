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
@Table(name = "server_http_endpoint_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerHttpEndpointMetric {

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

    @Column(nullable = false, length = 255)
    private String uri;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 3)
    private String status;

    @Column(name = "requests_count")
    private Long requestsCount;

    @Column(name = "requests_sum")
    private Double requestsSum;

    @Column(name = "requests_max")
    private Double requestsMax;

    @Builder
    public ServerHttpEndpointMetric(ServerRealtimeMetric realtimeMetric, Long serverId, LocalDateTime collectedAt,
                                    String uri, String method, String status,
                                    Long requestsCount, Double requestsSum, Double requestsMax) {
        this.realtimeMetric = realtimeMetric;
        this.serverId = serverId;
        this.collectedAt = collectedAt;
        this.uri = uri;
        this.method = method;
        this.status = status;
        this.requestsCount = requestsCount;
        this.requestsSum = requestsSum;
        this.requestsMax = requestsMax;
    }
}
