package com.moni.api.domain.server.stat.batch;

import static com.moni.api.domain.server.mapper.ServerRealtimeMetricMapper.isValidEndpointUri;

import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerStatBatchService {

    private static final String TIME_WINDOW_5M = "5M";
    private static final String TIME_WINDOW_1H = "1H";

    private final ServerRepository serverRepository;
    private final ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    private final StatJvmRepository statJvmRepository;
    private final StatHttpRepository statHttpRepository;
    private final StatHikariCpRepository statHikariCpRepository;
    private final StatThreadPoolRepository statThreadPoolRepository;

    @Scheduled(cron = "0 */5 * * * *")
    public void aggregateFiveMinuteStats() {
        LocalDateTime to = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime from = to.minusMinutes(5);

        List<Server> servers = serverRepository.findAll();
        for (Server server : servers) {
            try {
                aggregateFiveMinuteForServer(server, from, to);
            } catch (Exception e) {
                log.warn("Failed to aggregate 5M stats for server {}", server.getId(), e);
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void aggregateOneHourStats() {
        LocalDateTime to = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime from = to.minusHours(1);

        List<Server> servers = serverRepository.findAll();
        for (Server server : servers) {
            try {
                aggregateOneHourForServer(server, from, to);
            } catch (Exception e) {
                log.warn("Failed to aggregate 1H stats for server {}", server.getId(), e);
            }
        }
    }

    @Transactional
    public void aggregateFiveMinuteForServer(Server server, LocalDateTime from, LocalDateTime to) {
        Long serverId = server.getId();
        List<ServerRealtimeMetric> metrics = serverRealtimeMetricRepository
                .findAllByServerIdAndCollectedAtBetweenOrderByCollectedAtAsc(serverId, from, to);

        if (metrics.isEmpty()) {
            return;
        }

        aggregateJvm5M(server, metrics, to);
        aggregateHttp5M(server, metrics, from, to);
        aggregateHikariCp5M(server, metrics, to);
        aggregateThreadPool5M(server, metrics, to);
    }

    @Transactional
    public void aggregateOneHourForServer(Server server, LocalDateTime from, LocalDateTime to) {
        aggregateJvm1H(server, from, to);
        aggregateHttp1H(server, from, to);
        aggregateHikariCp1H(server, from, to);
        aggregateThreadPool1H(server, from, to);
    }

    // ==========================================
    // 5분 집계 로직 (원시 10초 데이터 -> 5M 집계)
    // ==========================================

    private void aggregateJvm5M(Server server, List<ServerRealtimeMetric> metrics, LocalDateTime statTime) {
        List<JvmMetric> jvmMetrics = metrics.stream()
                .map(ServerRealtimeMetric::getJvmMetric)
                .toList();

        if (jvmMetrics.isEmpty()) {
            return;
        }

        Long heapUsedAvg = Math.round(ServerStatMathUtils.avgLong(
                jvmMetrics.stream().map(JvmMetric::getJvmHeapUsedBytes).toList()));
        Long heapUsedMax = ServerStatMathUtils.maxLong(
                jvmMetrics.stream().map(JvmMetric::getJvmHeapUsedBytes).toList());
        Long oldGenUsedAvg = Math.round(ServerStatMathUtils.avgLong(
                jvmMetrics.stream().map(JvmMetric::getJvmOldGenUsedBytes).toList()));
        Integer threadBlockedMax = ServerStatMathUtils.maxInt(
                jvmMetrics.stream().map(JvmMetric::getJvmThreadsBlocked).toList());

        JvmMetric first = jvmMetrics.get(0);
        JvmMetric last = jvmMetrics.get(jvmMetrics.size() - 1);

        Long gcPauseCountSum = ServerStatMathUtils.computeDelta(
                first.getGcPauseSecondsCount(), last.getGcPauseSecondsCount());
        Double gcPauseSecondsSum = ServerStatMathUtils.computeDeltaDouble(
                first.getGcPauseSecondsSum(), last.getGcPauseSecondsSum());
        Double gcPauseMax = (gcPauseCountSum != null && gcPauseCountSum > 0 && gcPauseSecondsSum != null)
                ? (gcPauseSecondsSum / gcPauseCountSum)
                : 0.0;

        StatJvm stat = StatJvm.builder()
                .server(server)
                .timeWindow(TIME_WINDOW_5M)
                .statTime(statTime)
                .heapUsedAvg(heapUsedAvg)
                .heapUsedMax(heapUsedMax)
                .oldGenUsedAvg(oldGenUsedAvg)
                .gcPauseCountSum(gcPauseCountSum)
                .gcPauseSecondsSum(ServerStatMathUtils.round3(gcPauseSecondsSum))
                .gcPauseMax(ServerStatMathUtils.round3(gcPauseMax))
                .threadBlockedMax(threadBlockedMax != null ? threadBlockedMax : 0)
                .build();

        statJvmRepository.save(stat);
    }

    private void aggregateHttp5M(Server server, List<ServerRealtimeMetric> metrics,
            LocalDateTime from, LocalDateTime statTime) {
        List<ServerHttpEndpointMetric> allEndpoints = metrics.stream()
                .flatMap(m -> m.getHttpEndpoints().stream())
                .filter(ep -> isValidEndpointUri(ep.getUri()))
                .toList();

        if (allEndpoints.isEmpty()) {
            return;
        }

        Map<String, List<ServerHttpEndpointMetric>> byEndpoint = allEndpoints.stream()
                .collect(Collectors.groupingBy(
                        ep -> ep.getUri() + "|" + ep.getMethod(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        double durationSec = Math.max(1.0, Duration.between(from, statTime).toMillis() / 1000.0);

        for (List<ServerHttpEndpointMetric> group : byEndpoint.values()) {
            ServerHttpEndpointMetric sample = group.get(0);
            String uri = sample.getUri();
            String method = sample.getMethod();

            Map<String, List<ServerHttpEndpointMetric>> byStatus = group.stream()
                    .collect(Collectors.groupingBy(ServerHttpEndpointMetric::getStatus));

            long totalRequestsCount = 0L;
            double totalRequestsSum = 0.0;
            long errorCountSum = 0L;

            for (Map.Entry<String, List<ServerHttpEndpointMetric>> statusEntry : byStatus.entrySet()) {
                String status = statusEntry.getKey();
                List<ServerHttpEndpointMetric> statusList = statusEntry.getValue().stream()
                        .sorted((a, b) -> a.getCollectedAt().compareTo(b.getCollectedAt()))
                        .toList();

                ServerHttpEndpointMetric first = statusList.get(0);
                ServerHttpEndpointMetric last = statusList.get(statusList.size() - 1);

                long countDelta = ServerStatMathUtils.computeDelta(first.getRequestsCount(), last.getRequestsCount());
                double sumDelta = ServerStatMathUtils.computeDeltaDouble(first.getRequestsSum(), last.getRequestsSum());

                totalRequestsCount += countDelta;
                totalRequestsSum += sumDelta;

                if (status != null && (status.startsWith("4") || status.startsWith("5"))) {
                    errorCountSum += countDelta;
                }
            }

            Double maxResTimeSeconds = group.stream()
                    .map(ServerHttpEndpointMetric::getRequestsMax)
                    .filter(Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(0.0);
            double maxResTimeMs = ServerStatMathUtils.round1(maxResTimeSeconds * 1000.0);

            double rpsAvg = ServerStatMathUtils.round1((double) totalRequestsCount / durationSec);
            double rpsMax = calculate5MRpsMax(group, rpsAvg);

            double avgResTimeMs = totalRequestsCount > 0
                    ? ServerStatMathUtils.round1((totalRequestsSum / totalRequestsCount) * 1000.0)
                    : 0.0;

            double errorRateAvg = totalRequestsCount > 0
                    ? ServerStatMathUtils.round1(((double) errorCountSum / totalRequestsCount) * 100.0)
                    : 0.0;

            StatHttp stat = StatHttp.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_5M)
                    .statTime(statTime)
                    .uri(uri)
                    .method(method)
                    .totalRequestsCount(totalRequestsCount)
                    .totalRequestsSum(ServerStatMathUtils.round3(totalRequestsSum))
                    .rpsAvg(rpsAvg)
                    .rpsMax(rpsMax)
                    .avgResTimeMs(avgResTimeMs)
                    .maxResTimeMs(maxResTimeMs)
                    .errorCountSum(errorCountSum)
                    .errorRateAvg(errorRateAvg)
                    .build();

            statHttpRepository.save(stat);
        }
    }

    private double calculate5MRpsMax(List<ServerHttpEndpointMetric> group, double fallbackRps) {
        Map<LocalDateTime, List<ServerHttpEndpointMetric>> byTime = group.stream()
                .collect(Collectors.groupingBy(ServerHttpEndpointMetric::getCollectedAt));

        List<LocalDateTime> sortedTimes = byTime.keySet().stream().sorted().toList();
        if (sortedTimes.size() < 2) {
            return fallbackRps;
        }

        double maxRps = fallbackRps;
        for (int i = 1; i < sortedTimes.size(); i++) {
            LocalDateTime prevTime = sortedTimes.get(i - 1);
            LocalDateTime currTime = sortedTimes.get(i);

            double seconds = Duration.between(prevTime, currTime).toMillis() / 1000.0;
            if (seconds <= 0) {
                continue;
            }

            long prevSum = byTime.get(prevTime).stream()
                    .mapToLong(m -> m.getRequestsCount() != null ? m.getRequestsCount() : 0L)
                    .sum();
            long currSum = byTime.get(currTime).stream()
                    .mapToLong(m -> m.getRequestsCount() != null ? m.getRequestsCount() : 0L)
                    .sum();

            long delta = ServerStatMathUtils.computeDelta(prevSum, currSum);
            double sampleRps = delta / seconds;
            if (sampleRps > maxRps) {
                maxRps = sampleRps;
            }
        }

        return ServerStatMathUtils.round1(maxRps);
    }

    private void aggregateHikariCp5M(Server server, List<ServerRealtimeMetric> metrics, LocalDateTime statTime) {
        List<ServerHikariCpPoolMetric> allPools = metrics.stream()
                .flatMap(m -> m.getHikaricpPools().stream())
                .toList();

        if (allPools.isEmpty()) {
            return;
        }

        Map<String, List<ServerHikariCpPoolMetric>> byPool = allPools.stream()
                .collect(Collectors.groupingBy(
                        ServerHikariCpPoolMetric::getPoolName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<ServerHikariCpPoolMetric>> entry : byPool.entrySet()) {
            String poolName = entry.getKey();
            List<ServerHikariCpPoolMetric> poolMetrics = entry.getValue().stream()
                    .sorted((a, b) -> a.getCollectedAt().compareTo(b.getCollectedAt()))
                    .toList();

            Double activePoolAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avgInt(poolMetrics.stream().map(ServerHikariCpPoolMetric::getActive).toList()));
            Integer activePoolMax = ServerStatMathUtils.maxInt(
                    poolMetrics.stream().map(ServerHikariCpPoolMetric::getActive).toList());
            Integer pendingThreadsMax = ServerStatMathUtils.maxInt(
                    poolMetrics.stream().map(ServerHikariCpPoolMetric::getPending).toList());

            ServerHikariCpPoolMetric first = poolMetrics.get(0);
            ServerHikariCpPoolMetric last = poolMetrics.get(poolMetrics.size() - 1);
            Long timeoutDelta = ServerStatMathUtils.computeDelta(first.getTimeoutsTotal(), last.getTimeoutsTotal());

            StatHikariCp stat = StatHikariCp.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_5M)
                    .statTime(statTime)
                    .poolName(poolName)
                    .activePoolAvg(activePoolAvg != null ? activePoolAvg : 0.0)
                    .activePoolMax(activePoolMax != null ? activePoolMax : 0)
                    .pendingThreadsMax(pendingThreadsMax != null ? pendingThreadsMax : 0)
                    .timeoutCountSum(timeoutDelta != null ? timeoutDelta.intValue() : 0)
                    .build();

            statHikariCpRepository.save(stat);
        }
    }

    private void aggregateThreadPool5M(Server server, List<ServerRealtimeMetric> metrics, LocalDateTime statTime) {
        List<ServerExecutorMetric> allExecutors = metrics.stream()
                .flatMap(m -> m.getExecutors().stream())
                .toList();

        if (allExecutors.isEmpty()) {
            return;
        }

        Map<String, List<ServerExecutorMetric>> byExecutor = allExecutors.stream()
                .collect(Collectors.groupingBy(
                        ServerExecutorMetric::getName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<ServerExecutorMetric>> entry : byExecutor.entrySet()) {
            String name = entry.getKey();
            List<ServerExecutorMetric> execMetrics = entry.getValue();

            Double activeThreadsAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avgInt(execMetrics.stream().map(ServerExecutorMetric::getActive).toList()));
            Double maxThreadsAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avgInt(execMetrics.stream().map(ServerExecutorMetric::getMax).toList()));
            Double queuedTasksAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avgInt(execMetrics.stream().map(ServerExecutorMetric::getQueuedTasks).toList()));
            Integer queuedTasksMax = ServerStatMathUtils.maxInt(
                    execMetrics.stream().map(ServerExecutorMetric::getQueuedTasks).toList());

            StatThreadPool stat = StatThreadPool.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_5M)
                    .statTime(statTime)
                    .name(name)
                    .activeThreadsAvg(activeThreadsAvg != null ? activeThreadsAvg : 0.0)
                    .maxThreadsAvg(maxThreadsAvg != null ? maxThreadsAvg : 0.0)
                    .queuedTasksAvg(queuedTasksAvg != null ? queuedTasksAvg : 0.0)
                    .queuedTasksMax(queuedTasksMax != null ? queuedTasksMax : 0)
                    .build();

            statThreadPoolRepository.save(stat);
        }
    }

    // ==========================================
    // 1시간 집계 로직 (5M -> 1H 롤업 집계)
    // ==========================================

    private void aggregateJvm1H(Server server, LocalDateTime from, LocalDateTime to) {
        List<StatJvm> stats = statJvmRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                        server.getId(), TIME_WINDOW_5M, from.plusSeconds(1), to);

        if (stats.isEmpty()) {
            return;
        }

        Long heapUsedAvg = Math.round(ServerStatMathUtils.avgLong(
                stats.stream().map(StatJvm::getHeapUsedAvg).toList()));
        Long heapUsedMax = ServerStatMathUtils.maxLong(
                stats.stream().map(StatJvm::getHeapUsedMax).toList());
        Long oldGenUsedAvg = Math.round(ServerStatMathUtils.avgLong(
                stats.stream().map(StatJvm::getOldGenUsedAvg).toList()));
        Integer threadBlockedMax = ServerStatMathUtils.maxInt(
                stats.stream().map(StatJvm::getThreadBlockedMax).toList());

        long gcPauseCountSum = stats.stream()
                .map(StatJvm::getGcPauseCountSum)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        double gcPauseSecondsSum = ServerStatMathUtils.round3(stats.stream()
                .map(StatJvm::getGcPauseSecondsSum)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum());
        Double gcPauseMax = ServerStatMathUtils.max(
                stats.stream().map(StatJvm::getGcPauseMax).toList());

        StatJvm stat = StatJvm.builder()
                .server(server)
                .timeWindow(TIME_WINDOW_1H)
                .statTime(to)
                .heapUsedAvg(heapUsedAvg)
                .heapUsedMax(heapUsedMax)
                .oldGenUsedAvg(oldGenUsedAvg)
                .gcPauseCountSum(gcPauseCountSum)
                .gcPauseSecondsSum(gcPauseSecondsSum)
                .gcPauseMax(ServerStatMathUtils.round3(gcPauseMax))
                .threadBlockedMax(threadBlockedMax != null ? threadBlockedMax : 0)
                .build();

        statJvmRepository.save(stat);
    }

    private void aggregateHttp1H(Server server, LocalDateTime from, LocalDateTime to) {
        List<StatHttp> stats = statHttpRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                        server.getId(), TIME_WINDOW_5M, from.plusSeconds(1), to);

        if (stats.isEmpty()) {
            return;
        }

        Map<String, List<StatHttp>> byEndpoint = stats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getUri() + "|" + s.getMethod(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (List<StatHttp> group : byEndpoint.values()) {
            StatHttp sample = group.get(0);
            String uri = sample.getUri();
            String method = sample.getMethod();

            long totalRequestsCount = group.stream()
                    .map(StatHttp::getTotalRequestsCount)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            double totalRequestsSum = group.stream()
                    .map(StatHttp::getTotalRequestsSum)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            long errorCountSum = group.stream()
                    .map(StatHttp::getErrorCountSum)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();

            double rpsAvg = ServerStatMathUtils.round1((double) totalRequestsCount / 3600.0);
            Double rpsMax = group.stream()
                    .map(StatHttp::getRpsMax)
                    .filter(Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(0.0);
            Double maxResTimeMs = group.stream()
                    .map(StatHttp::getMaxResTimeMs)
                    .filter(Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(0.0);

            // 누적된 분자/분모 기반 가중 평균 응답시간 및 가중 에러율 산출 (평균의 평균 왜곡 방지)
            double avgResTimeMs = totalRequestsCount > 0
                    ? ServerStatMathUtils.round1((totalRequestsSum / totalRequestsCount) * 1000.0)
                    : 0.0;
            double errorRateAvg = totalRequestsCount > 0
                    ? ServerStatMathUtils.round1(((double) errorCountSum / totalRequestsCount) * 100.0)
                    : 0.0;

            StatHttp stat = StatHttp.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_1H)
                    .statTime(to)
                    .uri(uri)
                    .method(method)
                    .totalRequestsCount(totalRequestsCount)
                    .totalRequestsSum(ServerStatMathUtils.round3(totalRequestsSum))
                    .rpsAvg(rpsAvg)
                    .rpsMax(ServerStatMathUtils.round1(rpsMax))
                    .avgResTimeMs(avgResTimeMs)
                    .maxResTimeMs(ServerStatMathUtils.round1(maxResTimeMs))
                    .errorCountSum(errorCountSum)
                    .errorRateAvg(errorRateAvg)
                    .build();

            statHttpRepository.save(stat);
        }
    }

    private void aggregateHikariCp1H(Server server, LocalDateTime from, LocalDateTime to) {
        List<StatHikariCp> stats = statHikariCpRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                        server.getId(), TIME_WINDOW_5M, from.plusSeconds(1), to);

        if (stats.isEmpty()) {
            return;
        }

        Map<String, List<StatHikariCp>> byPool = stats.stream()
                .collect(Collectors.groupingBy(
                        StatHikariCp::getPoolName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<StatHikariCp>> entry : byPool.entrySet()) {
            String poolName = entry.getKey();
            List<StatHikariCp> group = entry.getValue();

            Double activePoolAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avg(group.stream().map(StatHikariCp::getActivePoolAvg).toList()));
            Integer activePoolMax = ServerStatMathUtils.maxInt(
                    group.stream().map(StatHikariCp::getActivePoolMax).toList());
            Integer pendingThreadsMax = ServerStatMathUtils.maxInt(
                    group.stream().map(StatHikariCp::getPendingThreadsMax).toList());
            int timeoutCountSum = group.stream()
                    .map(StatHikariCp::getTimeoutCountSum)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            StatHikariCp stat = StatHikariCp.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_1H)
                    .statTime(to)
                    .poolName(poolName)
                    .activePoolAvg(activePoolAvg != null ? activePoolAvg : 0.0)
                    .activePoolMax(activePoolMax != null ? activePoolMax : 0)
                    .pendingThreadsMax(pendingThreadsMax != null ? pendingThreadsMax : 0)
                    .timeoutCountSum(timeoutCountSum)
                    .build();

            statHikariCpRepository.save(stat);
        }
    }

    private void aggregateThreadPool1H(Server server, LocalDateTime from, LocalDateTime to) {
        List<StatThreadPool> stats = statThreadPoolRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                        server.getId(), TIME_WINDOW_5M, from.plusSeconds(1), to);

        if (stats.isEmpty()) {
            return;
        }

        Map<String, List<StatThreadPool>> byExecutor = stats.stream()
                .collect(Collectors.groupingBy(
                        StatThreadPool::getName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<StatThreadPool>> entry : byExecutor.entrySet()) {
            String name = entry.getKey();
            List<StatThreadPool> group = entry.getValue();

            Double activeThreadsAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avg(group.stream().map(StatThreadPool::getActiveThreadsAvg).toList()));
            Double maxThreadsAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avg(group.stream().map(StatThreadPool::getMaxThreadsAvg).toList()));
            Double queuedTasksAvg = ServerStatMathUtils.round1(
                    ServerStatMathUtils.avg(group.stream().map(StatThreadPool::getQueuedTasksAvg).toList()));
            Integer queuedTasksMax = ServerStatMathUtils.maxInt(
                    group.stream().map(StatThreadPool::getQueuedTasksMax).toList());

            StatThreadPool stat = StatThreadPool.builder()
                    .server(server)
                    .timeWindow(TIME_WINDOW_1H)
                    .statTime(to)
                    .name(name)
                    .activeThreadsAvg(activeThreadsAvg != null ? activeThreadsAvg : 0.0)
                    .maxThreadsAvg(maxThreadsAvg != null ? maxThreadsAvg : 0.0)
                    .queuedTasksAvg(queuedTasksAvg != null ? queuedTasksAvg : 0.0)
                    .queuedTasksMax(queuedTasksMax != null ? queuedTasksMax : 0)
                    .build();

            statThreadPoolRepository.save(stat);
        }
    }
}
