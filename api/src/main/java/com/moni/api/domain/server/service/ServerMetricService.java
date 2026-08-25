package com.moni.api.domain.server.service;

import static com.moni.api.domain.server.mapper.ServerRealtimeMetricMapper.isValidEndpointUri;

import com.moni.api.domain.server.dto.response.ServerHistoryMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerHistorySeriesDto;
import com.moni.api.domain.server.dto.response.ServerHistorySummaryDto;
import com.moni.api.domain.server.dto.response.ServerRealtimeCurrentDto;
import com.moni.api.domain.server.dto.response.ServerRealtimeMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerRealtimeSeriesDto;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.validator.ServerValidator;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerMetricService {

    private static final String TIME_WINDOW_1H = "1H";

    private final ServerValidator serverValidator;
    private final ServerRealtimeMetricRepository serverRealtimeMetricRepository;
    private final StatJvmRepository statJvmRepository;
    private final StatHttpRepository statHttpRepository;
    private final StatHikariCpRepository statHikariCpRepository;
    private final StatThreadPoolRepository statThreadPoolRepository;

    public ServerRealtimeMetricsResponse getServerRealtimeMetrics(Long serverId, Long userId, int limit) {
        serverValidator.validateAndGetServer(serverId, userId);

        List<ServerRealtimeMetric> metrics = serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(
                serverId, PageRequest.of(0, limit));

        if (metrics.isEmpty()) {
            return ServerRealtimeMetricsResponse.of(null, Collections.emptyList());
        }

        List<ServerRealtimeMetric> sortedMetrics = metrics.stream()
                .sorted(Comparator.comparing(ServerRealtimeMetric::getCollectedAt))
                .collect(Collectors.toList());

        List<ServerRealtimeSeriesDto> series = new ArrayList<>();
        Map<String, Long> prevEndpointCounts = new LinkedHashMap<>();
        LocalDateTime prevCollectedAt = null;

        for (int i = 0; i < sortedMetrics.size(); i++) {
            ServerRealtimeMetric m = sortedMetrics.get(i);
            double pointTotalRps = 0.0;

            if (prevCollectedAt != null && m.getHttpEndpoints() != null) {
                long secDiff = java.time.Duration.between(prevCollectedAt, m.getCollectedAt()).getSeconds();
                if (secDiff > 0) {
                    for (var ep : m.getHttpEndpoints()) {
                        if (!isValidEndpointUri(ep.getUri())) {
                            continue;
                        }
                        String epKey = ep.getUri() + "|" + ep.getMethod();
                        Long prevCount = prevEndpointCounts.get(epKey);
                        long curCount = ep.getRequestsCount() != null ? ep.getRequestsCount() : 0L;
                        if (prevCount != null && curCount >= prevCount) {
                            pointTotalRps += (double) (curCount - prevCount) / secDiff;
                        }
                    }
                }
            }

            if (m.getHttpEndpoints() != null) {
                for (var ep : m.getHttpEndpoints()) {
                    if (!isValidEndpointUri(ep.getUri())) {
                        continue;
                    }
                    String epKey = ep.getUri() + "|" + ep.getMethod();
                    prevEndpointCounts.put(epKey, ep.getRequestsCount() != null ? ep.getRequestsCount() : 0L);
                }
            }
            prevCollectedAt = m.getCollectedAt();

            series.add(ServerRealtimeSeriesDto.from(m, pointTotalRps));
        }

        if (series.size() > 1 && series.get(0).getTotalRps() == 0.0 && series.get(1).getTotalRps() > 0.0) {
            series.set(0, ServerRealtimeSeriesDto.from(sortedMetrics.get(0), series.get(1).getTotalRps()));
        }

        ServerRealtimeMetric latest = sortedMetrics.get(sortedMetrics.size() - 1);
        ServerRealtimeMetric secondLatest = sortedMetrics.size() > 1 ? sortedMetrics.get(sortedMetrics.size() - 2) : null;
        ServerRealtimeCurrentDto current = ServerRealtimeCurrentDto.from(latest, secondLatest);

        return ServerRealtimeMetricsResponse.of(current, series);
    }

    public ServerHistoryMetricsResponse getServerHistoryMetrics(Long serverId, Long userId, LocalDate date) {
        serverValidator.validateAndGetServer(serverId, userId);

        LocalDate queryDate = (date != null) ? date : LocalDate.now().minusDays(1);
        if (!queryDate.isBefore(LocalDate.now())) {
            throw new CustomException(ServerErrorCode.INVALID_HISTORICAL_DATE);
        }

        LocalDateTime from = queryDate.atStartOfDay();
        LocalDateTime to = queryDate.plusDays(1).atStartOfDay();

        List<StatJvm> jvmStats = statJvmRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(serverId, TIME_WINDOW_1H, from, to);
        List<StatHttp> httpStats = statHttpRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(serverId, TIME_WINDOW_1H, from, to);
        List<StatHikariCp> hikariStats = statHikariCpRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(serverId, TIME_WINDOW_1H, from, to);
        List<StatThreadPool> threadPoolStats = statThreadPoolRepository
                .findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(serverId, TIME_WINDOW_1H, from, to);

        ServerHistorySummaryDto summary = buildHistorySummary(jvmStats, httpStats, hikariStats, threadPoolStats);
        List<ServerHistorySeriesDto> series = buildHistorySeries(jvmStats, httpStats, hikariStats, threadPoolStats);

        return ServerHistoryMetricsResponse.of(serverId, queryDate, summary, series);
    }

    private ServerHistorySummaryDto buildHistorySummary(
            List<StatJvm> jvmStats,
            List<StatHttp> httpStats,
            List<StatHikariCp> hikariStats,
            List<StatThreadPool> threadPoolStats) {

        ServerHistorySummaryDto.JvmSummary jvmSummary = null;
        if (!jvmStats.isEmpty()) {
            double avgHeap = jvmStats.stream().mapToLong(s -> s.getHeapUsedAvg() != null ? s.getHeapUsedAvg() : 0L).average().orElse(0.0);
            long maxHeap = jvmStats.stream().mapToLong(s -> s.getHeapUsedMax() != null ? s.getHeapUsedMax() : 0L).max().orElse(0L);
            double avgOldGen = jvmStats.stream().mapToLong(s -> s.getOldGenUsedAvg() != null ? s.getOldGenUsedAvg() : 0L).average().orElse(0.0);
            long gcCountSum = jvmStats.stream().mapToLong(s -> s.getGcPauseCountSum() != null ? s.getGcPauseCountSum() : 0L).sum();
            double gcPauseSecondsSum = jvmStats.stream().mapToDouble(s -> s.getGcPauseSecondsSum() != null ? s.getGcPauseSecondsSum() : 0.0).sum();
            int threadBlockedMax = jvmStats.stream().mapToInt(s -> s.getThreadBlockedMax() != null ? s.getThreadBlockedMax() : 0).max().orElse(0);

            jvmSummary = ServerHistorySummaryDto.JvmSummary.builder()
                    .heapUsedAvgBytes(Math.round(avgHeap))
                    .heapUsedMaxBytes(maxHeap)
                    .oldGenUsedAvgBytes(Math.round(avgOldGen))
                    .gcPauseCountSum(gcCountSum)
                    .gcPauseSecondsSum(Math.round(gcPauseSecondsSum * 1000.0) / 1000.0)
                    .threadBlockedMax(threadBlockedMax)
                    .build();
        }

        Map<String, List<StatHttp>> httpByEndpoint = httpStats.stream()
                .filter(s -> s.getUri() != null)
                .collect(Collectors.groupingBy(s -> (s.getMethod() != null ? s.getMethod() : "GET") + " " + s.getUri(), LinkedHashMap::new, Collectors.toList()));

        List<ServerHistorySummaryDto.HttpEndpointSummary> httpSummaries = new ArrayList<>();
        for (Map.Entry<String, List<StatHttp>> entry : httpByEndpoint.entrySet()) {
            List<StatHttp> list = entry.getValue();
            StatHttp first = list.get(0);
            long totalReqCount = list.stream().mapToLong(s -> s.getTotalRequestsCount() != null ? s.getTotalRequestsCount() : 0L).sum();
            double rpsAvg = list.stream().mapToDouble(s -> s.getRpsAvg() != null ? s.getRpsAvg() : 0.0).average().orElse(0.0);
            double rpsMax = list.stream().mapToDouble(s -> s.getRpsMax() != null ? s.getRpsMax() : 0.0).max().orElse(0.0);

            double totalResTimeSum = list.stream()
                    .mapToDouble(s -> (s.getAvgResTimeMs() != null ? s.getAvgResTimeMs() : 0.0) * (s.getTotalRequestsCount() != null ? s.getTotalRequestsCount() : 0L))
                    .sum();
            double avgResTime = totalReqCount > 0 ? totalResTimeSum / totalReqCount : 0.0;
            double maxResTime = list.stream().mapToDouble(s -> s.getMaxResTimeMs() != null ? s.getMaxResTimeMs() : 0.0).max().orElse(0.0);

            double totalErrorSum = list.stream()
                    .mapToDouble(s -> (s.getErrorRateAvg() != null ? s.getErrorRateAvg() : 0.0) * (s.getTotalRequestsCount() != null ? s.getTotalRequestsCount() : 0L))
                    .sum();
            double errorRateAvg = totalReqCount > 0 ? totalErrorSum / totalReqCount : 0.0;

            httpSummaries.add(ServerHistorySummaryDto.HttpEndpointSummary.builder()
                    .uri(first.getUri())
                    .method(first.getMethod())
                    .totalRequestsCount(totalReqCount)
                    .rpsAvg(Math.round(rpsAvg * 10.0) / 10.0)
                    .rpsMax(Math.round(rpsMax * 10.0) / 10.0)
                    .avgResTimeMs(Math.round(avgResTime * 10.0) / 10.0)
                    .maxResTimeMs(Math.round(maxResTime * 10.0) / 10.0)
                    .errorRateAvg(Math.round(errorRateAvg * 100.0) / 100.0)
                    .build());
        }

        Map<String, List<StatHikariCp>> hikariByPool = hikariStats.stream()
                .filter(s -> s.getPoolName() != null)
                .collect(Collectors.groupingBy(StatHikariCp::getPoolName, LinkedHashMap::new, Collectors.toList()));

        List<ServerHistorySummaryDto.HikariCpPoolSummary> hikariSummaries = new ArrayList<>();
        for (Map.Entry<String, List<StatHikariCp>> entry : hikariByPool.entrySet()) {
            List<StatHikariCp> list = entry.getValue();
            double activeAvg = list.stream().mapToDouble(s -> s.getActivePoolAvg() != null ? s.getActivePoolAvg() : 0.0).average().orElse(0.0);
            int activeMax = list.stream().mapToInt(s -> s.getActivePoolMax() != null ? s.getActivePoolMax() : 0).max().orElse(0);
            int pendingMax = list.stream().mapToInt(s -> s.getPendingThreadsMax() != null ? s.getPendingThreadsMax() : 0).max().orElse(0);
            int timeoutSum = list.stream().mapToInt(s -> s.getTimeoutCountSum() != null ? s.getTimeoutCountSum() : 0).sum();

            hikariSummaries.add(ServerHistorySummaryDto.HikariCpPoolSummary.builder()
                    .poolName(entry.getKey())
                    .activePoolAvg(Math.round(activeAvg * 10.0) / 10.0)
                    .activePoolMax(activeMax)
                    .pendingThreadsMax(pendingMax)
                    .timeoutCountSum(timeoutSum)
                    .build());
        }

        Map<String, List<StatThreadPool>> poolByName = threadPoolStats.stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.groupingBy(StatThreadPool::getName, LinkedHashMap::new, Collectors.toList()));

        List<ServerHistorySummaryDto.ThreadPoolSummary> threadPoolSummaries = new ArrayList<>();
        for (Map.Entry<String, List<StatThreadPool>> entry : poolByName.entrySet()) {
            List<StatThreadPool> list = entry.getValue();
            double activeAvg = list.stream().mapToDouble(s -> s.getActiveThreadsAvg() != null ? s.getActiveThreadsAvg() : 0.0).average().orElse(0.0);
            double maxAvg = list.stream().mapToDouble(s -> s.getMaxThreadsAvg() != null ? s.getMaxThreadsAvg() : 0.0).average().orElse(0.0);
            double queuedAvg = list.stream().mapToDouble(s -> s.getQueuedTasksAvg() != null ? s.getQueuedTasksAvg() : 0.0).average().orElse(0.0);
            int queuedMax = list.stream().mapToInt(s -> s.getQueuedTasksMax() != null ? s.getQueuedTasksMax() : 0).max().orElse(0);

            threadPoolSummaries.add(ServerHistorySummaryDto.ThreadPoolSummary.builder()
                    .name(entry.getKey())
                    .activeThreadsAvg(Math.round(activeAvg * 10.0) / 10.0)
                    .maxThreadsAvg(Math.round(maxAvg * 10.0) / 10.0)
                    .queuedTasksAvg(Math.round(queuedAvg * 10.0) / 10.0)
                    .queuedTasksMax(queuedMax)
                    .build());
        }

        return ServerHistorySummaryDto.builder()
                .jvm(jvmSummary)
                .httpEndpoints(httpSummaries)
                .hikaricpPools(hikariSummaries)
                .executors(threadPoolSummaries)
                .build();
    }

    private List<ServerHistorySeriesDto> buildHistorySeries(
            List<StatJvm> jvmStats,
            List<StatHttp> httpStats,
            List<StatHikariCp> hikariStats,
            List<StatThreadPool> threadPoolStats) {

        Map<LocalDateTime, StatJvm> jvmByTime = jvmStats.stream()
                .collect(Collectors.toMap(StatJvm::getStatTime, s -> s, (a, b) -> a, LinkedHashMap::new));

        Map<LocalDateTime, List<StatHttp>> httpByTime = httpStats.stream()
                .collect(Collectors.groupingBy(StatHttp::getStatTime, LinkedHashMap::new, Collectors.toList()));

        Map<LocalDateTime, List<StatHikariCp>> hikariByTime = hikariStats.stream()
                .collect(Collectors.groupingBy(StatHikariCp::getStatTime, LinkedHashMap::new, Collectors.toList()));

        Map<LocalDateTime, List<StatThreadPool>> poolByTime = threadPoolStats.stream()
                .collect(Collectors.groupingBy(StatThreadPool::getStatTime, LinkedHashMap::new, Collectors.toList()));

        List<LocalDateTime> allTimes = Stream.of(
                        jvmStats.stream().map(StatJvm::getStatTime),
                        httpStats.stream().map(StatHttp::getStatTime),
                        hikariStats.stream().map(StatHikariCp::getStatTime),
                        threadPoolStats.stream().map(StatThreadPool::getStatTime))
                .flatMap(s -> s)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<ServerHistorySeriesDto> series = new ArrayList<>();
        for (LocalDateTime statTime : allTimes) {
            StatJvm jvm = jvmByTime.get(statTime);
            Long heapUsed = jvm != null ? jvm.getHeapUsedAvg() : null;
            Long heapMax = jvm != null ? jvm.getHeapUsedMax() : null;
            Long oldGen = jvm != null ? jvm.getOldGenUsedAvg() : null;
            Double gcPause = jvm != null ? jvm.getGcPauseSecondsSum() : null;

            List<StatHttp> hList = httpByTime.get(statTime);
            Double totalRps = null;
            Double avgLatency = null;
            if (hList != null && !hList.isEmpty()) {
                totalRps = hList.stream().mapToDouble(s -> s.getRpsAvg() != null ? s.getRpsAvg() : 0.0).sum();
                long hourlyTotalReqCount = hList.stream()
                        .mapToLong(s -> s.getTotalRequestsCount() != null ? s.getTotalRequestsCount() : 0L)
                        .sum();
                double hourlyResTimeSum = hList.stream()
                        .mapToDouble(s -> (s.getAvgResTimeMs() != null ? s.getAvgResTimeMs() : 0.0) * (s.getTotalRequestsCount() != null ? s.getTotalRequestsCount() : 0L))
                        .sum();
                avgLatency = hourlyTotalReqCount > 0 ? hourlyResTimeSum / hourlyTotalReqCount : 0.0;
            }

            List<StatHikariCp> hkList = hikariByTime.get(statTime);
            Double hikariActive = null;
            if (hkList != null && !hkList.isEmpty()) {
                hikariActive = hkList.stream().mapToDouble(s -> s.getActivePoolAvg() != null ? s.getActivePoolAvg() : 0.0).sum();
            }

            List<StatThreadPool> tpList = poolByTime.get(statTime);
            Double threadActive = null;
            if (tpList != null && !tpList.isEmpty()) {
                threadActive = tpList.stream().mapToDouble(s -> s.getActiveThreadsAvg() != null ? s.getActiveThreadsAvg() : 0.0).sum();
            }

            series.add(ServerHistorySeriesDto.builder()
                    .statTime(statTime)
                    .jvmHeapUsedBytes(heapUsed)
                    .jvmHeapMaxBytes(heapMax)
                    .jvmOldGenUsedBytes(oldGen)
                    .gcPauseSecondsSum(gcPause)
                    .totalRpsAvg(totalRps != null ? Math.round(totalRps * 10.0) / 10.0 : null)
                    .avgLatencyMs(avgLatency != null ? Math.round(avgLatency * 10.0) / 10.0 : null)
                    .hikaricpActiveAvg(hikariActive != null ? Math.round(hikariActive * 10.0) / 10.0 : null)
                    .executorActiveAvg(threadActive != null ? Math.round(threadActive * 10.0) / 10.0 : null)
                    .build());
        }

        return series;
    }
}

