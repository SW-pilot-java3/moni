package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DiskUsageCalculator {

    private DiskUsageCalculator() {
    }

    record Result(Double readBytesPerSec, Double writeBytesPerSec, Double utilizationPct) {
        static final Result EMPTY = new Result(null, null, null);
    }

    /**
     * 파일시스템 메트릭 목록으로부터 최대 디스크 점유율(%)을 계산한다.
     */
    static Double calculate(List<InstanceFileSystemMetric> filesystems) {
        if (filesystems == null) {
            return null;
        }

        Double maxUsagePct = null;

        for (InstanceFileSystemMetric filesystem : filesystems) {
            Long fsSizeBytes = filesystem.getFsSizeBytes();
            Long fsAvailBytes = filesystem.getFsAvailBytes();

            if (fsSizeBytes == null || fsSizeBytes <= 0 || fsAvailBytes == null) {
                continue;
            }

            double usagePct = (1 - ((double) fsAvailBytes / fsSizeBytes)) * 100;
            if (maxUsagePct == null || usagePct > maxUsagePct) {
                maxUsagePct = usagePct;
            }
        }

        return maxUsagePct;
    }

    /**
     * 이전/현재 디스크 I/O 메트릭 목록과 수집 시점으로부터 초당 읽기/쓰기 바이트 및 디스크 점유율(%)을 계산한다.
     */
    static Result calculate(List<InstanceDiskMetric> previous, List<InstanceDiskMetric> current,
                           LocalDateTime previousCollectedAt, LocalDateTime currentCollectedAt) {
        if (previous == null || previous.isEmpty() || current == null || current.isEmpty()) {
            return Result.EMPTY;
        }

        if (previousCollectedAt == null || currentCollectedAt == null) {
            return Result.EMPTY;
        }

        double intervalSeconds = ChronoUnit.MILLIS.between(previousCollectedAt, currentCollectedAt) / 1000.0;
        if (intervalSeconds <= 0) {
            return Result.EMPTY;
        }

        Map<String, InstanceDiskMetric> previousByDevice = previous.stream()
                .collect(Collectors.toMap(InstanceDiskMetric::getDeviceName, Function.identity(), (a, b) -> a));

        long readBytesDelta = 0;
        long writeBytesDelta = 0;
        double ioTimeDelta = 0;
        boolean hasReadWrite = false;
        boolean hasIoTime = false;

        for (InstanceDiskMetric currentDevice : current) {
            InstanceDiskMetric previousDevice = previousByDevice.get(currentDevice.getDeviceName());
            if (previousDevice == null) {
                continue;
            }

            if (currentDevice.getReadBytesTotal() != null && previousDevice.getReadBytesTotal() != null) {
                readBytesDelta += Math.max(0, currentDevice.getReadBytesTotal() - previousDevice.getReadBytesTotal());
                hasReadWrite = true;
            }
            if (currentDevice.getWrittenBytesTotal() != null && previousDevice.getWrittenBytesTotal() != null) {
                writeBytesDelta += Math.max(0, currentDevice.getWrittenBytesTotal() - previousDevice.getWrittenBytesTotal());
                hasReadWrite = true;
            }
            if (currentDevice.getIoTimeSecondsTotal() != null && previousDevice.getIoTimeSecondsTotal() != null) {
                ioTimeDelta += Math.max(0, currentDevice.getIoTimeSecondsTotal() - previousDevice.getIoTimeSecondsTotal());
                hasIoTime = true;
            }
        }

        Double readBytesPerSec = hasReadWrite ? readBytesDelta / intervalSeconds : null;
        Double writeBytesPerSec = hasReadWrite ? writeBytesDelta / intervalSeconds : null;
        Double utilizationPct = hasIoTime ? Math.min(100.0, (ioTimeDelta / intervalSeconds) * 100) : null;

        return new Result(readBytesPerSec, writeBytesPerSec, utilizationPct);
    }
}
