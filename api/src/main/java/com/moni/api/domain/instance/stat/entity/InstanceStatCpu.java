package com.moni.api.domain.instance.stat.entity;

import com.moni.api.domain.instance.entity.Instance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "instance_stat_cpu",
        indexes = @Index(name = "idx_instance_stat_cpu_instance_window_time", columnList = "instance_id, time_window, stat_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceStatCpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Instance instance;

    @Column(name = "time_window", nullable = false, length = 10)
    private String timeWindow;

    @Column(name = "stat_time", nullable = false)
    private LocalDateTime statTime;

    @Column(name = "cpu_usage_avg")
    private Double cpuUsageAvg;

    @Column(name = "cpu_usage_max")
    private Double cpuUsageMax;

    @Column(name = "cpu_iowait_avg")
    private Double cpuIowaitAvg;

    @Builder
    public InstanceStatCpu(Instance instance, String timeWindow, LocalDateTime statTime,
                            Double cpuUsageAvg, Double cpuUsageMax, Double cpuIowaitAvg) {
        this.instance = instance;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.cpuUsageAvg = cpuUsageAvg;
        this.cpuUsageMax = cpuUsageMax;
        this.cpuIowaitAvg = cpuIowaitAvg;
    }
}