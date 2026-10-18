package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class DiskLatencyCalculator {

    private static final Pattern VIRTUAL_DEVICE_PATTERN = Pattern.compile("loop\\d+|dm-\\d+|ram\\d+");

    private DiskLatencyCalculator() {
    }

    static Double calculate(List<InstanceDiskMetric> previous, List<InstanceDiskMetric> current) {
        if (previous == null || current == null) {
            return null;
        }

        Map<String, InstanceDiskMetric> previousByDevice = previous.stream()
                .collect(Collectors.toMap(InstanceDiskMetric::getDeviceName, Function.identity(), (a, b) -> a));

        Double maxLatencyMs = null;

        for (InstanceDiskMetric currentDevice : current) {
            String deviceName = currentDevice.getDeviceName();
            if (deviceName == null || VIRTUAL_DEVICE_PATTERN.matcher(deviceName).matches()) {
                continue;
            }

            InstanceDiskMetric previousDevice = previousByDevice.get(deviceName);
            if (previousDevice == null) {
                continue; // 신규 등장 장치, 이번 윈도우 판정 스킵
            }

            Double latencyMs = calculateDeviceLatency(previousDevice, currentDevice);
            if (latencyMs != null && (maxLatencyMs == null || latencyMs > maxLatencyMs)) {
                maxLatencyMs = latencyMs;
            }
        }

        return maxLatencyMs;
    }

    private static Double calculateDeviceLatency(InstanceDiskMetric previous, InstanceDiskMetric current) {
        Long prevReads = previous.getReadsTotal();
        Long prevWrites = previous.getWritesTotal();
        Double prevIoTime = previous.getIoTimeSecondsTotal();
        Long currReads = current.getReadsTotal();
        Long currWrites = current.getWritesTotal();
        Double currIoTime = current.getIoTimeSecondsTotal();

        if (prevReads == null || prevWrites == null || prevIoTime == null
                || currReads == null || currWrites == null || currIoTime == null) {
            return null;
        }

        long opsDelta = (currReads + currWrites) - (prevReads + prevWrites);
        double ioTimeDelta = currIoTime - prevIoTime;

        if (opsDelta <= 0 || ioTimeDelta < 0) {
            return null;
        }

        return (ioTimeDelta * 1000) / opsDelta;
    }
}
