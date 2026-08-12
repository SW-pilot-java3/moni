package com.moni.api.domain.instance.entity;

import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Table(name = "instance_thresholds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceThreshold extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_key", length = 50, nullable = false)
    private MetricKey metricKey;

    @Column(name = "warning_val", nullable = false)
    private Double warningVal;

    @Column(name = "critical_val", nullable = false)
    private Double criticalVal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Instance instance;

    @Builder
    public InstanceThreshold(Instance instance, MetricKey metricKey, Double warningVal, Double criticalVal) {
        this.instance = instance;
        this.metricKey = metricKey;
        this.warningVal = warningVal;
        this.criticalVal = criticalVal;
    }

    public void update(Double warningVal, Double criticalVal) {
        this.warningVal = warningVal;
        this.criticalVal = criticalVal;
    }
}
