package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.dto.InstanceMetricStreamEvent;
import com.moni.api.domain.instance.dto.InstanceRealtimeMetricCreateRequest;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceCpuMetric;
import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.embeddable.CpuMetrics;
import com.moni.api.domain.instance.entity.embeddable.MemoryMetrics;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceCpuMetricRepository;
import com.moni.api.domain.instance.repository.InstanceDiskMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceFileSystemMetricRepository;
import com.moni.api.domain.instance.repository.InstanceNetworkMetricsRepository;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InstanceRealtimeMetricService {

    private final InstanceRepository instanceRepository;
    private final InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;
    private final InstanceCpuMetricRepository instanceCpuMetricRepository;
    private final InstanceDiskMetricsRepository instanceDiskMetricsRepository;
    private final InstanceFileSystemMetricRepository instanceFileSystemMetricRepository;
    private final InstanceNetworkMetricsRepository instanceNetworkMetricsRepository;
    private final InstanceThresholdEvaluationService instanceThresholdEvaluationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void recordMetric(Long instanceId, InstanceRealtimeMetricCreateRequest request) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        InstanceRealtimeMetricCreateRequest.InstancePayload payload = request.instance();

        CpuMetrics cpuMetrics = CpuMetrics.builder()
                .cpuSecondsTotal(payload.cpuSecondsTotal())
                .cpuIdleSecondsTotal(payload.cpuIdleSecondsTotal())
                .cpuIowaitSecondsTotal(payload.cpuIOWaitSecondsTotal())
                .build();

        MemoryMetrics memoryMetrics = MemoryMetrics.builder()
                .memTotalBytes(payload.memTotalBytes())
                .memFreeBytes(payload.memFreeBytes())
                .memAvailableBytes(payload.memAvailableBytes())
                .buffersBytes(payload.buffersBytes())
                .cachedBytes(payload.cachedBytes())
                .swapTotalBytes(payload.swapTotalBytes())
                .swapFreeBytes(payload.swapFreeBytes())
                .build();

        Optional<InstanceRealtimeMetric> previousRealtimeMetric = instanceRealtimeMetricRepository
                .findFirstByInstanceIdAndCollectedAtLessThanOrderByCollectedAtDesc(instanceId, request.collectedAt());
        InstanceRealtimeMetric previousMetric = previousRealtimeMetric.orElse(null);
        CpuMetrics previousCpuMetrics = previousMetric != null ? previousMetric.getCpuMetrics() : null;
        Double cpuUsagePct = CpuUsageCalculator.calculate(previousCpuMetrics, cpuMetrics);

        InstanceRealtimeMetric metric = InstanceRealtimeMetric.builder()
                .instance(instance)
                .collectedAt(request.collectedAt())
                .cpuMetrics(cpuMetrics)
                .memoryMetrics(memoryMetrics)
                .build();

        try {
            instanceRealtimeMetricRepository.save(metric);
        } catch (DataIntegrityViolationException e) {
            log.info("이미 처리된 메트릭 push - instanceId={}, collectedAt={}", instanceId, request.collectedAt());
            return;
        }

        for (InstanceRealtimeMetricCreateRequest.CoreCpu coreCpu : payload.cpus()) {
            InstanceCpuMetric cpuMetric = InstanceCpuMetric.builder()
                    .realtimeMetric(metric)
                    .collectedAt(request.collectedAt())
                    .coreId(coreCpu.coreId())
                    .cpuSecondsTotal(coreCpu.cpuSecondsTotal())
                    .cpuIdleSecondsTotal(coreCpu.cpuIdleSecondsTotal())
                    .cpuIowaitSecondsTotal(coreCpu.cpuIowaitSecondsTotal())
                    .build();
            instanceCpuMetricRepository.save(cpuMetric);
        }

        List<InstanceDiskMetric> currentDiskMetrics = new ArrayList<>();
        for (InstanceRealtimeMetricCreateRequest.DiskDevice diskDevice : payload.disks()) {
            InstanceDiskMetric diskMetric = InstanceDiskMetric.builder()
                    .realtimeMetric(metric)
                    .collectedAt(request.collectedAt())
                    .deviceName(diskDevice.deviceName())
                    .readsTotal(diskDevice.readsTotal())
                    .writesTotal(diskDevice.writesTotal())
                    .readBytesTotal(diskDevice.readBytesTotal())
                    .writtenBytesTotal(diskDevice.writtenBytesTotal())
                    .ioTimeSecondsTotal(diskDevice.ioTimeSecondsTotal())
                    .build();
            currentDiskMetrics.add(instanceDiskMetricsRepository.save(diskMetric));
        }

        List<InstanceFileSystemMetric> fileSystemMetrics = new ArrayList<>();
        for (InstanceRealtimeMetricCreateRequest.FileSystemMount fileSystemMount : payload.filesystems()) {
            InstanceFileSystemMetric fileSystemMetric = InstanceFileSystemMetric.builder()
                    .realtimeMetric(metric)
                    .collectedAt(request.collectedAt())
                    .mountPoint(fileSystemMount.mountPoint())
                    .fsSizeBytes(fileSystemMount.fsSizeBytes())
                    .fsAvailBytes(fileSystemMount.fsAvailBytes())
                    .build();
            instanceFileSystemMetricRepository.save(fileSystemMetric);
            fileSystemMetrics.add(fileSystemMetric);
        }

        List<InstanceNetworkMetric> currentNetworkMetrics = new ArrayList<>();
        for (InstanceRealtimeMetricCreateRequest.NetworkInterfaceMetric networkInterface : payload.networks()) {
            InstanceNetworkMetric networkMetric = InstanceNetworkMetric.builder()
                    .realtimeMetric(metric)
                    .collectedAt(request.collectedAt())
                    .interfaceName(networkInterface.interfaceName())
                    .rxBytesTotal(networkInterface.rxBytesTotal())
                    .txBytesTotal(networkInterface.txBytesTotal())
                    .rxErrorsTotal(networkInterface.rxErrorsTotal())
                    .txErrorsTotal(networkInterface.txErrorsTotal())
                    .build();
            currentNetworkMetrics.add(instanceNetworkMetricsRepository.save(networkMetric));
        }

        DiskUsageCalculator.Result diskUsage = DiskUsageCalculator.Result.EMPTY;
        NetworkUsageCalculator.Result networkUsage = NetworkUsageCalculator.Result.EMPTY;
        List<InstanceDiskMetric> previousDiskMetrics = List.of();
        List<InstanceNetworkMetric> previousNetworkMetrics = List.of();

        if (previousMetric != null) {
            previousDiskMetrics = instanceDiskMetricsRepository
                    .findAllByRealtimeMetricId(previousMetric.getId());
            previousNetworkMetrics = instanceNetworkMetricsRepository
                    .findAllByRealtimeMetricId(previousMetric.getId());

            diskUsage = DiskUsageCalculator.calculate(
                    previousDiskMetrics, currentDiskMetrics, previousMetric.getCollectedAt(), request.collectedAt());
            networkUsage = NetworkUsageCalculator.calculate(
                    previousNetworkMetrics, currentNetworkMetrics, previousMetric.getCollectedAt(), request.collectedAt());
        }

        Double diskLatencyMs = DiskLatencyCalculator.calculate(previousDiskMetrics, currentDiskMetrics);
        Double netErrorRate = NetErrorRateCalculator.calculate(previousNetworkMetrics, currentNetworkMetrics);

        instanceThresholdEvaluationService.evaluate(instanceId, request.collectedAt(), cpuUsagePct, memoryMetrics,
                fileSystemMetrics, diskLatencyMs, netErrorRate);

        eventPublisher.publishEvent(new InstanceMetricStreamEvent(
                instanceId, request.collectedAt(), cpuUsagePct, memoryMetrics.getMemAvailableBytes(),
                diskUsage.readBytesPerSec(), diskUsage.writeBytesPerSec(), diskUsage.utilizationPct(),
                networkUsage.rxBytesPerSec(), networkUsage.txBytesPerSec(), networkUsage.errorsPerSec()));
    }
}