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
        name = "instance_stat_network",
        indexes = @Index(name = "idx_instance_stat_network_instance_window_time", columnList = "instance_id, time_window, stat_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceStatNetwork {

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

    @Column(name = "rx_mbps_avg")
    private Double rxMbpsAvg;

    @Column(name = "tx_mbps_avg")
    private Double txMbpsAvg;

    @Column(name = "errors_sum")
    private Integer errorsSum;

    @Builder
    public InstanceStatNetwork(Instance instance, String timeWindow, LocalDateTime statTime,
                                Double rxMbpsAvg, Double txMbpsAvg, Integer errorsSum) {
        this.instance = instance;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.rxMbpsAvg = rxMbpsAvg;
        this.txMbpsAvg = txMbpsAvg;
        this.errorsSum = errorsSum;
    }
}