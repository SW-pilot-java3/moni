package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.dto.*;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceDiskMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceNetworkMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.instance.stat.entity.InstanceStatCpu;
import com.moni.api.domain.instance.stat.entity.InstanceStatDisk;
import com.moni.api.domain.instance.stat.entity.InstanceStatMemory;
import com.moni.api.domain.instance.stat.entity.InstanceStatNetwork;
import com.moni.api.domain.instance.stat.repository.InstanceStatCpuRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatDiskRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatMemoryRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatNetworkRepository;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.CommonErrorCode;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InstanceService {

    private static final int DEFAULT_REALTIME_METRIC_LIMIT = 30;
    private static final String TIME_WINDOW_1H = "1H";

    private final InstanceRepository instanceRepository;
    private final InstanceThresholdRepository instanceThresholdRepository;
    private final InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;
    private final InstanceDiskMetricsRepository instanceDiskMetricsRepository;
    private final InstanceNetworkMetricsRepository instanceNetworkMetricsRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final InstanceStatCpuRepository instanceStatCpuRepository;
    private final InstanceStatMemoryRepository instanceStatMemoryRepository;
    private final InstanceStatDiskRepository instanceStatDiskRepository;
    private final InstanceStatNetworkRepository instanceStatNetworkRepository;

    public List<InstanceListItemResponse> getInstances(Long userId) {
        return instanceRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(instance -> InstanceListItemResponse.of(instance, serverRepository.countByInstanceId(instance.getId())))
                .toList();
    }

    @Transactional
    public InstanceCreateResponse createInstance(Long userId, InstanceCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.ENTITY_NOT_FOUND));

        Instance instance = Instance.builder()
                .name(request.name())
                .ip(request.ip())
                .user(user)
                .build();
        Instance saved = instanceRepository.save(instance);

        List<InstanceThreshold> defaultThresholds = Arrays.stream(MetricKey.values())
                .map(metricKey -> InstanceThreshold.builder()
                        .instance(saved)
                        .metricKey(metricKey)
                        .warningVal(metricKey.getDefaultWarningVal())
                        .criticalVal(metricKey.getDefaultCriticalVal())
                        .build())
                .toList();
        instanceThresholdRepository.saveAll(defaultThresholds);

        return InstanceCreateResponse.from(saved);
    }

    public Instance getInstanceById(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));
    }

    public Instance getInstanceOwnedBy(Long instanceId, Long userId) {
        Instance instance = getInstanceById(instanceId);
        if (!instance.getUser().getId().equals(userId)) {
            throw new CustomException(InstanceErrorCode.INSTANCE_ACCESS_DENIED);
        }
        return instance;
    }

    public List<InstanceThresholdResponse> getThresholds(Long instanceId, Long userId) {
        Instance instance = getInstanceOwnedBy(instanceId, userId);

        Map<MetricKey, InstanceThreshold> thresholdsByMetricKey = instanceThresholdRepository
                .findAllByInstanceId(instanceId).stream()
                .collect(Collectors.toMap(InstanceThreshold::getMetricKey, Function.identity()));

        return Arrays.stream(MetricKey.values())
                .map(metricKey -> {
                    InstanceThreshold threshold = thresholdsByMetricKey.get(metricKey);
                    return threshold != null
                            ? InstanceThresholdResponse.from(threshold)
                            : InstanceThresholdResponse.defaultOf(metricKey);
                })
                .toList();
    }

    public List<InstanceRealtimeMetricResponse> getRecentRealtimeMetrics(Long instanceId, Long userId) {
        getInstanceOwnedBy(instanceId, userId);

        List<InstanceRealtimeMetric> recentDesc = instanceRealtimeMetricRepository
                .findAllByInstanceIdOrderByCollectedAtDesc(instanceId, PageRequest.of(0, DEFAULT_REALTIME_METRIC_LIMIT));

        List<InstanceRealtimeMetric> metrics = new ArrayList<>(recentDesc);
        Collections.reverse(metrics);

        List<Long> realtimeMetricIds = metrics.stream().map(InstanceRealtimeMetric::getId).toList();
        Map<Long, List<InstanceDiskMetric>> diskMetricsByRealtimeId = instanceDiskMetricsRepository
                .findAllByRealtimeMetricIdIn(realtimeMetricIds).stream()
                .collect(Collectors.groupingBy(d -> d.getRealtimeMetric().getId()));
        Map<Long, List<InstanceNetworkMetric>> networkMetricsByRealtimeId = instanceNetworkMetricsRepository
                .findAllByRealtimeMetricIdIn(realtimeMetricIds).stream()
                .collect(Collectors.groupingBy(n -> n.getRealtimeMetric().getId()));

        List<InstanceRealtimeMetricResponse> responses = new ArrayList<>();
        InstanceRealtimeMetric previous = null;
        for (InstanceRealtimeMetric current : metrics) {
            Double cpuUsagePct = previous == null
                    ? null
                    : CpuUsageCalculator.calculate(previous.getCpuMetrics(), current.getCpuMetrics());

            DiskUsageCalculator.Result diskUsage = DiskUsageCalculator.Result.EMPTY;
            NetworkUsageCalculator.Result networkUsage = NetworkUsageCalculator.Result.EMPTY;
            if (previous != null) {
                diskUsage = DiskUsageCalculator.calculate(
                        diskMetricsByRealtimeId.getOrDefault(previous.getId(), List.of()),
                        diskMetricsByRealtimeId.getOrDefault(current.getId(), List.of()),
                        previous.getCollectedAt(), current.getCollectedAt());
                networkUsage = NetworkUsageCalculator.calculate(
                        networkMetricsByRealtimeId.getOrDefault(previous.getId(), List.of()),
                        networkMetricsByRealtimeId.getOrDefault(current.getId(), List.of()),
                        previous.getCollectedAt(), current.getCollectedAt());
            }

            responses.add(new InstanceRealtimeMetricResponse(
                    current.getCollectedAt(),
                    cpuUsagePct,
                    current.getMemoryMetrics().getMemAvailableBytes(),
                    diskUsage.readBytesPerSec(),
                    diskUsage.writeBytesPerSec(),
                    diskUsage.utilizationPct(),
                    networkUsage.rxBytesPerSec(),
                    networkUsage.txBytesPerSec(),
                    networkUsage.errorsPerSec()
            ));
            previous = current;
        }

        return responses;
    }

    @Transactional
    public InstanceUpdateResponse updateInstance(Long instanceId, Long userId, InstanceUpdateRequest request) {
        Instance instance = getInstanceOwnedBy(instanceId, userId);

        instance.update(request.name(), request.ip());

        return InstanceUpdateResponse.from(instance);
    }

    @Transactional
    public void deleteInstance(Long instanceId, Long userId) {
        Instance instance = getInstanceOwnedBy(instanceId, userId);

        instanceRepository.delete(instance);
    }

    @Transactional
    public InstanceThresholdUpdateResponse updateThresholds(Long instanceId, Long userId, InstanceThresholdUpdateRequest request) {
        Instance instance = getInstanceOwnedBy(instanceId, userId);

        Map<MetricKey, InstanceThreshold> thresholdsByMetricKey = instanceThresholdRepository
                .findAllByInstanceId(instance.getId()).stream()
                .collect(Collectors.toMap(InstanceThreshold::getMetricKey, Function.identity()));

        for (InstanceThresholdUpdateRequest.ThresholdItem item : request.thresholds()) {
            InstanceThreshold threshold = thresholdsByMetricKey.get(item.metricKey());
            if (threshold == null) {
                throw new CustomException(InstanceErrorCode.THRESHOLD_NOT_FOUND);
            }

            threshold.update(item.warningValue(), item.criticalValue());
        }

        return new InstanceThresholdUpdateResponse(
                instance.getId(), request.thresholds().size(), LocalDateTime.now());
    }

    public InstanceHistoryMetricsResponse getInstanceHistoryMetrics(Long instanceId, Long userId, LocalDate date) {
        getInstanceOwnedBy(instanceId, userId);

        LocalDate queryDate = (date != null) ? date : LocalDate.now().minusDays(1);
        if (!queryDate.isBefore(LocalDate.now())) {
            throw new CustomException(InstanceErrorCode.INVALID_HISTORICAL_DATE);
        }

        LocalDateTime from = queryDate.atStartOfDay();
        LocalDateTime to = queryDate.atTime(LocalTime.MAX);

        List<InstanceStatCpu> cpuStats = instanceStatCpuRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW_1H, from, to);
        List<InstanceStatMemory> memoryStats = instanceStatMemoryRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW_1H, from, to);
        List<InstanceStatDisk> diskStats = instanceStatDiskRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW_1H, from, to);
        List<InstanceStatNetwork> networkStats = instanceStatNetworkRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW_1H, from, to);

        InstanceHistorySummaryDto summary = buildHistorySummary(cpuStats, memoryStats, diskStats, networkStats);
        List<InstanceHistorySeriesDto> series = buildHistorySeries(cpuStats, memoryStats, diskStats, networkStats);

        return InstanceHistoryMetricsResponse.of(instanceId, queryDate, summary, series);
    }

    private InstanceHistorySummaryDto buildHistorySummary(
            List<InstanceStatCpu> cpuStats,
            List<InstanceStatMemory> memoryStats,
            List<InstanceStatDisk> diskStats,
            List<InstanceStatNetwork> networkStats) {

        InstanceHistorySummaryDto.CpuSummary cpuSummary = null;
        if (!cpuStats.isEmpty()) {
            double usageAvg = cpuStats.stream().mapToDouble(s -> s.getCpuUsageAvg() != null ? s.getCpuUsageAvg() : 0.0).average().orElse(0.0);
            double usageMax = cpuStats.stream().mapToDouble(s -> s.getCpuUsageMax() != null ? s.getCpuUsageMax() : 0.0).max().orElse(0.0);
            double iowaitAvg = cpuStats.stream().mapToDouble(s -> s.getCpuIowaitAvg() != null ? s.getCpuIowaitAvg() : 0.0).average().orElse(0.0);
            cpuSummary = new InstanceHistorySummaryDto.CpuSummary(
                    Math.round(usageAvg * 10.0) / 10.0, Math.round(usageMax * 10.0) / 10.0, Math.round(iowaitAvg * 10.0) / 10.0);
        }

        InstanceHistorySummaryDto.MemorySummary memorySummary = null;
        if (!memoryStats.isEmpty()) {
            long availAvg = Math.round(memoryStats.stream()
                    .mapToLong(s -> s.getMemAvailableAvg() != null ? s.getMemAvailableAvg() : 0L).average().orElse(0.0));
            long availMin = memoryStats.stream()
                    .mapToLong(s -> s.getMemAvailableMin() != null ? s.getMemAvailableMin() : 0L).min().orElse(0L);
            double swapMax = memoryStats.stream()
                    .mapToDouble(s -> s.getSwapUsedMax() != null ? s.getSwapUsedMax() : 0.0).max().orElse(0.0);
            memorySummary = new InstanceHistorySummaryDto.MemorySummary(availAvg, availMin, Math.round(swapMax * 10.0) / 10.0);
        }

        InstanceHistorySummaryDto.DiskSummary diskSummary = null;
        if (!diskStats.isEmpty()) {
            double readAvg = diskStats.stream().mapToDouble(s -> s.getReadIopsAvg() != null ? s.getReadIopsAvg() : 0.0).average().orElse(0.0);
            double writeAvg = diskStats.stream().mapToDouble(s -> s.getWriteIopsAvg() != null ? s.getWriteIopsAvg() : 0.0).average().orElse(0.0);
            double utilMax = diskStats.stream().mapToDouble(s -> s.getDiskUtilMax() != null ? s.getDiskUtilMax() : 0.0).max().orElse(0.0);
            double usedPctMax = diskStats.stream().mapToDouble(s -> s.getDiskUsedPctMax() != null ? s.getDiskUsedPctMax() : 0.0).max().orElse(0.0);
            diskSummary = new InstanceHistorySummaryDto.DiskSummary(
                    Math.round(readAvg * 10.0) / 10.0, Math.round(writeAvg * 10.0) / 10.0,
                    Math.round(utilMax * 10.0) / 10.0, Math.round(usedPctMax * 10.0) / 10.0);
        }

        InstanceHistorySummaryDto.NetworkSummary networkSummary = null;
        if (!networkStats.isEmpty()) {
            double rxAvg = networkStats.stream().mapToDouble(s -> s.getRxMbpsAvg() != null ? s.getRxMbpsAvg() : 0.0).average().orElse(0.0);
            double txAvg = networkStats.stream().mapToDouble(s -> s.getTxMbpsAvg() != null ? s.getTxMbpsAvg() : 0.0).average().orElse(0.0);
            int errorsSum = networkStats.stream().mapToInt(s -> s.getErrorsSum() != null ? s.getErrorsSum() : 0).sum();
            networkSummary = new InstanceHistorySummaryDto.NetworkSummary(
                    Math.round(rxAvg * 10.0) / 10.0, Math.round(txAvg * 10.0) / 10.0, errorsSum);
        }

        return new InstanceHistorySummaryDto(cpuSummary, memorySummary, diskSummary, networkSummary);
    }

    private List<InstanceHistorySeriesDto> buildHistorySeries(
            List<InstanceStatCpu> cpuStats,
            List<InstanceStatMemory> memoryStats,
            List<InstanceStatDisk> diskStats,
            List<InstanceStatNetwork> networkStats) {

        Map<LocalDateTime, InstanceStatCpu> cpuByTime = cpuStats.stream()
                .collect(Collectors.toMap(InstanceStatCpu::getStatTime, s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<LocalDateTime, InstanceStatMemory> memoryByTime = memoryStats.stream()
                .collect(Collectors.toMap(InstanceStatMemory::getStatTime, s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<LocalDateTime, InstanceStatDisk> diskByTime = diskStats.stream()
                .collect(Collectors.toMap(InstanceStatDisk::getStatTime, s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<LocalDateTime, InstanceStatNetwork> networkByTime = networkStats.stream()
                .collect(Collectors.toMap(InstanceStatNetwork::getStatTime, s -> s, (a, b) -> a, LinkedHashMap::new));

        List<LocalDateTime> allTimes = Stream.of(
                        cpuStats.stream().map(InstanceStatCpu::getStatTime),
                        memoryStats.stream().map(InstanceStatMemory::getStatTime),
                        diskStats.stream().map(InstanceStatDisk::getStatTime),
                        networkStats.stream().map(InstanceStatNetwork::getStatTime))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<InstanceHistorySeriesDto> series = new ArrayList<>();
        for (LocalDateTime statTime : allTimes) {
            InstanceStatCpu cpu = cpuByTime.get(statTime);
            InstanceStatMemory memory = memoryByTime.get(statTime);
            InstanceStatDisk disk = diskByTime.get(statTime);
            InstanceStatNetwork network = networkByTime.get(statTime);

            series.add(new InstanceHistorySeriesDto(
                    statTime,
                    cpu != null ? cpu.getCpuUsageAvg() : null,
                    cpu != null ? cpu.getCpuUsageMax() : null,
                    memory != null ? memory.getMemAvailableAvg() : null,
                    disk != null ? disk.getReadIopsAvg() : null,
                    disk != null ? disk.getWriteIopsAvg() : null,
                    disk != null ? disk.getDiskUsedPctMax() : null,
                    network != null ? network.getRxMbpsAvg() : null,
                    network != null ? network.getTxMbpsAvg() : null
            ));
        }

        return series;
    }
}

