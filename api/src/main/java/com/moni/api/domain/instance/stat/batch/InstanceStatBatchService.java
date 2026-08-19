package com.moni.api.domain.instance.stat.batch;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;
import com.moni.api.domain.instance.repository.InstanceDiskMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceFileSystemMetricRepository;
import com.moni.api.domain.instance.repository.InstanceNetworkMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.stat.entity.InstanceStatCpu;
import com.moni.api.domain.instance.stat.entity.InstanceStatDisk;
import com.moni.api.domain.instance.stat.entity.InstanceStatMemory;
import com.moni.api.domain.instance.stat.entity.InstanceStatNetwork;
import com.moni.api.domain.instance.stat.repository.InstanceStatCpuRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatDiskRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatMemoryRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatNetworkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceStatBatchService {

    private static final String TIME_WINDOW = "5M";

    private final InstanceRepository instanceRepository;
    private final InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;
    private final InstanceDiskMetricsRepository instanceDiskMetricsRepository;
    private final InstanceFileSystemMetricRepository instanceFileSystemMetricRepository;
    private final InstanceNetworkMetricsRepository instanceNetworkMetricsRepository;

    private final InstanceStatCpuRepository instanceStatCpuRepository;
    private final InstanceStatMemoryRepository instanceStatMemoryRepository;
    private final InstanceStatDiskRepository instanceStatDiskRepository;
    private final InstanceStatNetworkRepository instanceStatNetworkRepository;

    @Scheduled(cron = "0 */5 * * * *")
    public void aggregateFiveMinuteStats() {
        LocalDateTime to = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime from = to.minusMinutes(5);

        List<Instance> instances = instanceRepository.findAll();
        for (Instance instance : instances) {
            try {
                aggregateForInstance(instance, from, to);
            } catch (Exception e) {
                log.warn("Failed to aggregate stats for instance {}", instance.getId(), e);
            }
        }
    }

    @Transactional
    public void aggregateForInstance(Instance instance, LocalDateTime from, LocalDateTime to) {
        Long instanceId = instance.getId();
        LocalDateTime statTime = to;

        List<InstanceRealtimeMetric> realtimeMetrics = instanceRealtimeMetricRepository
                .findAllByInstanceIdAndCollectedAtBetweenOrderByCollectedAtAsc(instanceId, from, to);

        aggregateCpu(instance, realtimeMetrics, statTime);
        aggregateMemory(instance, realtimeMetrics, statTime);

        List<InstanceDiskMetric> diskMetrics = instanceDiskMetricsRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to);
        List<InstanceFileSystemMetric> fileSystemMetrics = instanceFileSystemMetricRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to);
        aggregateDisk(instance, diskMetrics, fileSystemMetrics, statTime);

        List<InstanceNetworkMetric> networkMetrics = instanceNetworkMetricsRepository
                .findAllByInstanceIdAndCollectedAtBetween(instanceId, from, to);
        aggregateNetwork(instance, networkMetrics, statTime);
    }

    private void aggregateCpu(Instance instance, List<InstanceRealtimeMetric> metrics, LocalDateTime statTime) {
        if (metrics.size() < 2) {
            return;
        }

        List<Double> usages = StatMathUtils.pairwise(metrics, InstanceRealtimeMetric::getCpuMetrics,
                this::cpuUsagePct);
        List<Double> iowaitRatios = StatMathUtils.pairwise(metrics, InstanceRealtimeMetric::getCpuMetrics,
                this::cpuIowaitPct);

        if (usages.isEmpty()) {
            return;
        }

        InstanceStatCpu stat = InstanceStatCpu.builder()
                .instance(instance)
                .timeWindow(TIME_WINDOW)
                .statTime(statTime)
                .cpuUsageAvg(StatMathUtils.avg(usages))
                .cpuUsageMax(StatMathUtils.max(usages))
                .cpuIowaitAvg(StatMathUtils.avg(iowaitRatios))
                .build();
        instanceStatCpuRepository.save(stat);
    }

    private Double cpuUsagePct(CpuMetrics prev, CpuMetrics curr) {
        if (prev.getCpuSecondsTotal() == null || prev.getCpuIdleSecondsTotal() == null
                || curr.getCpuSecondsTotal() == null || curr.getCpuIdleSecondsTotal() == null) {
            return null;
        }
        double totalDelta = curr.getCpuSecondsTotal() - prev.getCpuSecondsTotal();
        double idleDelta = curr.getCpuIdleSecondsTotal() - prev.getCpuIdleSecondsTotal();
        if (totalDelta <= 0) {
            return null;
        }
        return (1 - (idleDelta / totalDelta)) * 100;
    }

    private Double cpuIowaitPct(CpuMetrics prev, CpuMetrics curr) {
        if (prev.getCpuSecondsTotal() == null || prev.getCpuIowaitSecondsTotal() == null
                || curr.getCpuSecondsTotal() == null || curr.getCpuIowaitSecondsTotal() == null) {
            return null;
        }
        double totalDelta = curr.getCpuSecondsTotal() - prev.getCpuSecondsTotal();
        double iowaitDelta = curr.getCpuIowaitSecondsTotal() - prev.getCpuIowaitSecondsTotal();
        if (totalDelta <= 0) {
            return null;
        }
        return (iowaitDelta / totalDelta) * 100;
    }

    private void aggregateMemory(Instance instance, List<InstanceRealtimeMetric> metrics, LocalDateTime statTime) {
        if (metrics.isEmpty()) {
            return;
        }

        List<Long> availables = metrics.stream()
                .map(m -> m.getMemoryMetrics().getMemAvailableBytes())
                .filter(Objects::nonNull)
                .toList();

        List<Double> swapUsedPcts = metrics.stream()
                .map(m -> {
                    Long total = m.getMemoryMetrics().getSwapTotalBytes();
                    Long free = m.getMemoryMetrics().getSwapFreeBytes();
                    if (total == null || free == null || total == 0) {
                        return null;
                    }
                    return (1 - ((double) free / total)) * 100;
                })
                .filter(Objects::nonNull)
                .toList();

        if (availables.isEmpty()) {
            return;
        }

        InstanceStatMemory stat = InstanceStatMemory.builder()
                .instance(instance)
                .timeWindow(TIME_WINDOW)
                .statTime(statTime)
                .memAvailableAvg((long) availables.stream().mapToLong(Long::longValue).average().orElse(0))
                .memAvailableMin(availables.stream().mapToLong(Long::longValue).min().orElse(0))
                .swapUsedMax(StatMathUtils.max(swapUsedPcts))
                .build();
        instanceStatMemoryRepository.save(stat);
    }

    private void aggregateDisk(Instance instance, List<InstanceDiskMetric> diskMetrics,
                                List<InstanceFileSystemMetric> fileSystemMetrics, LocalDateTime statTime) {
        if (diskMetrics.isEmpty() && fileSystemMetrics.isEmpty()) {
            return;
        }

        // 같은 수집 시각의 디바이스별 스냅샷을 시간순으로 나열한 뒤, 스냅샷 간 델타로
        // IOPS(전 디바이스 합산)와 util(디바이스 중 최댓값)을 계산한다.
        Map<LocalDateTime, Map<String, InstanceDiskMetric>> byCollectedAt = diskMetrics.stream()
                .collect(Collectors.groupingBy(InstanceDiskMetric::getCollectedAt,
                        Collectors.toMap(InstanceDiskMetric::getDeviceName, m -> m, (a, b) -> a)));

        List<LocalDateTime> sortedTimes = byCollectedAt.keySet().stream().sorted().toList();

        List<Double> readIopsSamples = new ArrayList<>();
        List<Double> writeIopsSamples = new ArrayList<>();
        List<Double> utilSamples = new ArrayList<>();

        for (int i = 1; i < sortedTimes.size(); i++) {
            Map<String, InstanceDiskMetric> prevSnapshot = byCollectedAt.get(sortedTimes.get(i - 1));
            Map<String, InstanceDiskMetric> currSnapshot = byCollectedAt.get(sortedTimes.get(i));

            double seconds = Duration.between(sortedTimes.get(i - 1), sortedTimes.get(i)).toMillis() / 1000.0;
            if (seconds <= 0) {
                continue;
            }

            double readIopsSum = 0;
            double writeIopsSum = 0;
            double maxUtilAtThisTime = Double.NEGATIVE_INFINITY;
            boolean anyIops = false;
            boolean anyUtil = false;

            for (Map.Entry<String, InstanceDiskMetric> entry : currSnapshot.entrySet()) {
                InstanceDiskMetric curr = entry.getValue();
                InstanceDiskMetric prev = prevSnapshot.get(entry.getKey());
                if (prev == null) {
                    continue;
                }

                if (prev.getReadsTotal() != null && curr.getReadsTotal() != null) {
                    readIopsSum += (curr.getReadsTotal() - prev.getReadsTotal()) / seconds;
                    anyIops = true;
                }
                if (prev.getWritesTotal() != null && curr.getWritesTotal() != null) {
                    writeIopsSum += (curr.getWritesTotal() - prev.getWritesTotal()) / seconds;
                    anyIops = true;
                }
                if (prev.getIoTimeSecondsTotal() != null && curr.getIoTimeSecondsTotal() != null) {
                    double util = ((curr.getIoTimeSecondsTotal() - prev.getIoTimeSecondsTotal()) / seconds) * 100;
                    maxUtilAtThisTime = Math.max(maxUtilAtThisTime, util);
                    anyUtil = true;
                }
            }

            if (anyIops) {
                readIopsSamples.add(readIopsSum);
                writeIopsSamples.add(writeIopsSum);
            }
            if (anyUtil) {
                utilSamples.add(maxUtilAtThisTime);
            }
        }

        Double diskUsedPctMax = fileSystemMetrics.stream()
                .map(f -> {
                    Long size = f.getFsSizeBytes();
                    Long avail = f.getFsAvailBytes();
                    if (size == null || avail == null || size == 0) {
                        return null;
                    }
                    return (1 - ((double) avail / size)) * 100;
                })
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        InstanceStatDisk stat = InstanceStatDisk.builder()
                .instance(instance)
                .timeWindow(TIME_WINDOW)
                .statTime(statTime)
                .readIopsAvg(StatMathUtils.avg(readIopsSamples))
                .writeIopsAvg(StatMathUtils.avg(writeIopsSamples))
                .diskUtilMax(StatMathUtils.max(utilSamples))
                .diskUsedPctMax(diskUsedPctMax)
                .build();
        instanceStatDiskRepository.save(stat);
    }

    private void aggregateNetwork(Instance instance, List<InstanceNetworkMetric> metrics, LocalDateTime statTime) {
        if (metrics.isEmpty()) {
            return;
        }

        // Disk와 동일하게, 같은 수집 시각의 인터페이스별 스냅샷을 시간순으로 나열한 뒤
        // 스냅샷 간 델타로 rx/tx(전 인터페이스 합산)와 에러 발생 건수를 계산한다.
        Map<LocalDateTime, Map<String, InstanceNetworkMetric>> byCollectedAt = metrics.stream()
                .collect(Collectors.groupingBy(InstanceNetworkMetric::getCollectedAt,
                        Collectors.toMap(InstanceNetworkMetric::getInterfaceName, m -> m, (a, b) -> a)));

        List<LocalDateTime> sortedTimes = byCollectedAt.keySet().stream().sorted().toList();

        List<Double> rxMbpsSamples = new ArrayList<>();
        List<Double> txMbpsSamples = new ArrayList<>();
        long errorsSum = 0;

        for (int i = 1; i < sortedTimes.size(); i++) {
            Map<String, InstanceNetworkMetric> prevSnapshot = byCollectedAt.get(sortedTimes.get(i - 1));
            Map<String, InstanceNetworkMetric> currSnapshot = byCollectedAt.get(sortedTimes.get(i));

            double seconds = Duration.between(sortedTimes.get(i - 1), sortedTimes.get(i)).toMillis() / 1000.0;
            if (seconds <= 0) {
                continue;
            }

            double rxMbpsSum = 0;
            double txMbpsSum = 0;
            boolean anyTraffic = false;

            for (Map.Entry<String, InstanceNetworkMetric> entry : currSnapshot.entrySet()) {
                InstanceNetworkMetric curr = entry.getValue();
                InstanceNetworkMetric prev = prevSnapshot.get(entry.getKey());
                if (prev == null) {
                    continue;
                }

                if (prev.getRxBytesTotal() != null && curr.getRxBytesTotal() != null) {
                    long rxDelta = curr.getRxBytesTotal() - prev.getRxBytesTotal();
                    rxMbpsSum += (rxDelta * 8.0 / seconds) / 1_000_000.0;
                    anyTraffic = true;
                }
                if (prev.getTxBytesTotal() != null && curr.getTxBytesTotal() != null) {
                    long txDelta = curr.getTxBytesTotal() - prev.getTxBytesTotal();
                    txMbpsSum += (txDelta * 8.0 / seconds) / 1_000_000.0;
                    anyTraffic = true;
                }
                if (prev.getRxErrorsTotal() != null && curr.getRxErrorsTotal() != null) {
                    errorsSum += Math.max(0, curr.getRxErrorsTotal() - prev.getRxErrorsTotal());
                }
                if (prev.getTxErrorsTotal() != null && curr.getTxErrorsTotal() != null) {
                    errorsSum += Math.max(0, curr.getTxErrorsTotal() - prev.getTxErrorsTotal());
                }
            }

            if (anyTraffic) {
                rxMbpsSamples.add(rxMbpsSum);
                txMbpsSamples.add(txMbpsSum);
            }
        }

        InstanceStatNetwork stat = InstanceStatNetwork.builder()
                .instance(instance)
                .timeWindow(TIME_WINDOW)
                .statTime(statTime)
                .rxMbpsAvg(StatMathUtils.avg(rxMbpsSamples))
                .txMbpsAvg(StatMathUtils.avg(txMbpsSamples))
                .errorsSum((int) errorsSum)
                .build();
        instanceStatNetworkRepository.save(stat);
    }
}