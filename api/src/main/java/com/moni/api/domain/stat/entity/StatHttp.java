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
    name = "stat_http",
    indexes = {
        @Index(name = "idx_stat_http_server_window_time", columnList = "server_id, time_window, stat_time DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatHttp {

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

    @Column(name = "uri", length = 255)
    private String uri;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "total_requests_count")
    private Long totalRequestsCount;

    @Column(name = "total_requests_sum")
    private Double totalRequestsSum;

    @Column(name = "rps_avg")
    private Double rpsAvg;

    @Column(name = "rps_max")
    private Double rpsMax;

    @Column(name = "avg_res_time_ms")
    private Double avgResTimeMs;

    @Column(name = "max_res_time_ms")
    private Double maxResTimeMs;

    @Column(name = "error_count_sum")
    private Long errorCountSum;

    @Column(name = "error_rate_avg")
    private Double errorRateAvg;

    @Builder
    public StatHttp(Server server, String timeWindow, LocalDateTime statTime,
            String uri, String method, Long totalRequestsCount, Double totalRequestsSum,
            Double rpsAvg, Double rpsMax, Double avgResTimeMs, Double maxResTimeMs,
            Long errorCountSum, Double errorRateAvg) {
        this.server = server;
        this.timeWindow = timeWindow;
        this.statTime = statTime;
        this.uri = uri;
        this.method = method;
        this.totalRequestsCount = totalRequestsCount;
        this.totalRequestsSum = totalRequestsSum;
        this.rpsAvg = rpsAvg;
        this.rpsMax = rpsMax;
        this.avgResTimeMs = avgResTimeMs;
        this.maxResTimeMs = maxResTimeMs;
        this.errorCountSum = errorCountSum;
        this.errorRateAvg = errorRateAvg;
    }
}
