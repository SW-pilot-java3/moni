package com.moni.api.domain.server.service;

import com.moni.api.domain.server.entity.ServerExecutorMetric;

import java.util.List;

final class ThreadPoolQueueUsageCalculator {

    /**
     * Spring Boot의 ThreadPoolTaskExecutor는 queue-capacity를 명시적으로 설정하지 않으면
     * 기본값이 Integer.MAX_VALUE(사실상 무제한)다. 사람이 직접 설정하는 큐 용량은 아무리 커도
     * 이 값에는 한참 못 미치므로, queueRemaining이 이 임계값 이상이면 무제한 큐로 보고 판정에서 제외한다.
     */
    private static final int UNBOUNDED_QUEUE_THRESHOLD = 1_000_000;

    private ThreadPoolQueueUsageCalculator() {
    }

    static Double calculate(List<ServerExecutorMetric> executors) {
        if (executors == null) {
            return null;
        }

        Double maxUsagePct = null;

        for (ServerExecutorMetric executor : executors) {
            Integer queuedTasks = executor.getQueuedTasks();
            Integer queueRemaining = executor.getQueueRemaining();

            if (queuedTasks == null || queueRemaining == null) {
                continue;
            }
            if (queueRemaining >= UNBOUNDED_QUEUE_THRESHOLD) {
                continue;
            }

            int capacity = queuedTasks + queueRemaining;
            if (capacity <= 0) {
                continue;
            }

            double usagePct = ((double) queuedTasks / capacity) * 100;
            if (maxUsagePct == null || usagePct > maxUsagePct) {
                maxUsagePct = usagePct;
            }
        }

        return maxUsagePct;
    }
}
