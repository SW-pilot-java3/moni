package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HikariCpPoolDto {

    private String poolName;
    private Integer active;
    private Integer idle;
    private Integer pending;
    private Integer max;
    private Long timeoutsTotal;

    public static HikariCpPoolDto from(ServerHikariCpPoolMetric metric) {
        return HikariCpPoolDto.builder()
                .poolName(metric.getPoolName())
                .active(metric.getActive() != null ? metric.getActive() : 0)
                .idle(metric.getIdle() != null ? metric.getIdle() : 0)
                .pending(metric.getPending() != null ? metric.getPending() : 0)
                .max(metric.getMax() != null ? metric.getMax() : 0)
                .timeoutsTotal(metric.getTimeoutsTotal() != null ? metric.getTimeoutsTotal() : 0L)
                .build();
    }
}
