package com.moni.api.domain.server.entity;

import com.moni.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "server_thresholds",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_server_thresholds_server_metric", columnNames = {"server_id", "metric_key"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerThreshold extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_key", nullable = false, length = 50)
    private ServerMetricKey metricKey;

    @Column(name = "warning_val", nullable = false)
    private Double warningValue;

    @Column(name = "critical_val", nullable = false)
    private Double criticalValue;

    @Builder
    public ServerThreshold(Server server, ServerMetricKey metricKey, Double warningValue, Double criticalValue) {
        this.server = server;
        this.metricKey = metricKey;
        this.warningValue = warningValue;
        this.criticalValue = criticalValue;
    }

    public void updateThreshold(Double warningValue, Double criticalValue) {
        this.warningValue = warningValue;
        this.criticalValue = criticalValue;
    }
}
