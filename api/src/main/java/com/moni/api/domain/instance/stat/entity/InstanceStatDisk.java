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
        name = "instance_stat_disk",
        indexes = @Index(name = "idx_instance_stat_disk_instance_window_time", columnList = "instance_id, time_window, stat_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceStatDisk {

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

    @Column(name = "read_iops_avg")
    private Double readIopsAvg;

    @Column(name = "write_iops_avg")
    private Double writeIopsAvg;

    @Column(name = "disk_util_max")
    private Double diskUtilMax;

    @Column(name = "disk_used_pct_max")
    private Double diskUsedPctMax;

    @Builder
    public InstanceStatDisk(Instance instance, String timeWindow, LocalDateTime statTime,
                             Double readIopsAvg, Double writeIopsAvg, Double diskUtilMax, Double diskUsedPctMax) {
        this.instance = instance;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.readIopsAvg = readIopsAvg;
        this.writeIopsAvg = writeIopsAvg;
        this.diskUtilMax = diskUtilMax;
        this.diskUsedPctMax = diskUsedPctMax;
    }
}