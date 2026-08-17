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
    name = "stat_threadpool",
    indexes = {
        @Index(name = "idx_stat_threadpool_server_window_time", columnList = "server_id, time_window, stat_time DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatThreadPool {

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

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "active_threads_avg")
    private Double activeThreadsAvg;

    @Column(name = "max_threads_avg")
    private Double maxThreadsAvg;

    @Column(name = "queued_tasks_avg")
    private Double queuedTasksAvg;

    @Column(name = "queued_tasks_max")
    private Integer queuedTasksMax;

    @Builder
    public StatThreadPool(Server server, String timeWindow, LocalDateTime statTime,
            String name, Double activeThreadsAvg, Double maxThreadsAvg,
            Double queuedTasksAvg, Integer queuedTasksMax) {
        this.server = server;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.name = name;
        this.activeThreadsAvg = activeThreadsAvg;
        this.maxThreadsAvg = maxThreadsAvg;
        this.queuedTasksAvg = queuedTasksAvg;
        this.queuedTasksMax = queuedTasksMax;
    }
}
