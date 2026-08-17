package com.moni.api.domain.stat.entity;

import com.moni.api.domain.server.entity.Server;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "stat_jvm",
    indexes = {
        @Index(name = "idx_stat_jvm_server_window_time", columnList = "server_id, time_window, stat_time DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatJvm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Server server;

    @Column(name = "time_window", nullable = false, length = 10)
    private String timeWindow;

    @Column(name = "stat_time", nullable = false)
    private LocalDateTime statTime;

    @Column(name = "heap_used_avg")
    private Long heapUsedAvg;

    @Column(name = "heap_used_max")
    private Long heapUsedMax;

    @Column(name = "old_gen_used_avg")
    private Long oldGenUsedAvg;

    @Column(name = "gc_pause_count_sum")
    private Long gcPauseCountSum;

    @Column(name = "gc_pause_seconds_sum")
    private Double gcPauseSecondsSum;

    @Column(name = "gc_pause_max")
    private Double gcPauseMax;

    @Column(name = "thread_blocked_max")
    private Integer threadBlockedMax;

    @Builder
    public StatJvm(Server server, String timeWindow, LocalDateTime statTime,
            Long heapUsedAvg, Long heapUsedMax, Long oldGenUsedAvg,
            Long gcPauseCountSum, Double gcPauseSecondsSum, Double gcPauseMax,
            Integer threadBlockedMax) {
        this.server = server;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.heapUsedAvg = heapUsedAvg;
        this.heapUsedMax = heapUsedMax;
        this.oldGenUsedAvg = oldGenUsedAvg;
        this.gcPauseCountSum = gcPauseCountSum;
        this.gcPauseSecondsSum = gcPauseSecondsSum;
        this.gcPauseMax = gcPauseMax;
        this.threadBlockedMax = threadBlockedMax;
    }
}
