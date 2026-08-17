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
    name = "stat_hikaricp",
    indexes = {
        @Index(name = "idx_stat_hikaricp_server_window_time", columnList = "server_id, time_window, stat_time DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatHikariCp {

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

    @Column(name = "pool_name", length = 50)
    private String poolName;

    @Column(name = "active_pool_avg")
    private Double activePoolAvg;

    @Column(name = "active_pool_max")
    private Integer activePoolMax;

    @Column(name = "pending_threads_max")
    private Integer pendingThreadsMax;

    @Column(name = "timeout_count_sum")
    private Integer timeoutCountSum;

    @Builder
    public StatHikariCp(Server server, String timeWindow, LocalDateTime statTime,
            String poolName, Double activePoolAvg, Integer activePoolMax,
            Integer pendingThreadsMax, Integer timeoutCountSum) {
        this.server = server;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.poolName = poolName;
        this.activePoolAvg = activePoolAvg;
        this.activePoolMax = activePoolMax;
        this.pendingThreadsMax = pendingThreadsMax;
        this.timeoutCountSum = timeoutCountSum;
    }
}
