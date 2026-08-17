package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.ServerExecutorMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutorDto {

    private String name;
    private Integer active;
    private Integer max;
    private Integer queuedTasks;
    private Integer queueRemaining;

    public static ExecutorDto from(ServerExecutorMetric metric) {
        return ExecutorDto.builder()
                .name(metric.getName())
                .active(metric.getActive() != null ? metric.getActive() : 0)
                .max(metric.getMax() != null ? metric.getMax() : 0)
                .queuedTasks(metric.getQueuedTasks() != null ? metric.getQueuedTasks() : 0)
                .queueRemaining(metric.getQueueRemaining() != null ? metric.getQueueRemaining() : 0)
                .build();
    }
}
