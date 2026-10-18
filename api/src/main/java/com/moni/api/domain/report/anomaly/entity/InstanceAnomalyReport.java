package com.moni.api.domain.report.anomaly.entity;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.global.entity.BaseTimeEntity;
import com.moni.api.global.threshold.ThresholdSeverity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "instance_anomaly_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceAnomalyReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Instance instance;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_key", nullable = false, length = 30)
    private MetricKey metricKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private ThresholdSeverity severity;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Builder
    public InstanceAnomalyReport(Instance instance, MetricKey metricKey, ThresholdSeverity severity,
            Double value, LocalDateTime collectedAt, String summary, String content) {
        this.instance = instance;
        this.metricKey = metricKey;
        this.severity = severity;
        this.value = value;
        this.collectedAt = collectedAt;
        this.summary = summary;
        this.content = content;
    }
}
