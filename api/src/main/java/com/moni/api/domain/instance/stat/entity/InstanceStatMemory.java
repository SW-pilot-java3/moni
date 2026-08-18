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
        name = "instance_stat_memory",
        indexes = @Index(name = "idx_instance_stat_memory_instance_window_time", columnList = "instance_id, time_window, stat_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceStatMemory {

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

    @Column(name = "mem_available_avg")
    private Long memAvailableAvg;

    @Column(name = "mem_available_min")
    private Long memAvailableMin;

    @Column(name = "swap_used_max")
    private Double swapUsedMax;

    @Builder
    public InstanceStatMemory(Instance instance, String timeWindow, LocalDateTime statTime,
                               Long memAvailableAvg, Long memAvailableMin, Double swapUsedMax) {
        this.instance = instance;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.memAvailableAvg = memAvailableAvg;
        this.memAvailableMin = memAvailableMin;
        this.swapUsedMax = swapUsedMax;
    }
}