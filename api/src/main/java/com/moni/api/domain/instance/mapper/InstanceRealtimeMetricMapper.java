package com.moni.api.domain.instance.mapper;

import com.moni.api.domain.instance.dto.InstanceRealtimeMetricCreateRequest;
import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class InstanceRealtimeMetricMapper {

    private InstanceRealtimeMetricMapper() {
    }

    public static InstanceRealtimeMetricCreateRequest from(MetricRecordRequest request) {
        MetricRecordRequest.InstanceMetricPayload payload = request.getInstance();

        InstanceRealtimeMetricCreateRequest.InstancePayload instancePayload =
                new InstanceRealtimeMetricCreateRequest.InstancePayload(
                        payload.getCpuSecondsTotal(),
                        payload.getCpuIdleSecondsTotal(),
                        payload.getCpuIowaitSecondsTotal(),
                        payload.getMemTotalBytes(),
                        payload.getMemFreeBytes(),
                        payload.getMemAvailableBytes(),
                        payload.getBuffersBytes(),
                        payload.getCachedBytes(),
                        payload.getSwapTotalBytes(),
                        payload.getSwapFreeBytes(),
                        toCpus(payload.getCpus()),
                        toDisks(payload.getDisks()),
                        toFilesystems(payload.getFilesystems()),
                        toNetworks(payload.getNetworks()));

        LocalDateTime collectedAt = LocalDateTime.ofInstant(request.getCollectedAt(), ZoneId.systemDefault());
        return new InstanceRealtimeMetricCreateRequest(collectedAt, instancePayload);
    }

    private static List<InstanceRealtimeMetricCreateRequest.CoreCpu> toCpus(
            List<MetricRecordRequest.InstanceCpuPayload> cpus) {
        return cpus.stream()
                .map(cpu -> new InstanceRealtimeMetricCreateRequest.CoreCpu(
                        cpu.getCoreId(),
                        cpu.getCpuSecondsTotal(),
                        cpu.getCpuIdleSecondsTotal(),
                        cpu.getCpuIowaitSecondsTotal()))
                .toList();
    }

    private static List<InstanceRealtimeMetricCreateRequest.DiskDevice> toDisks(
            List<MetricRecordRequest.InstanceDiskPayload> disks) {
        return disks.stream()
                .map(disk -> new InstanceRealtimeMetricCreateRequest.DiskDevice(
                        disk.getDeviceName(),
                        disk.getReadsTotal(),
                        disk.getWritesTotal(),
                        disk.getReadBytesTotal(),
                        disk.getWrittenBytesTotal(),
                        disk.getIoTimeSecondsTotal()))
                .toList();
    }

    private static List<InstanceRealtimeMetricCreateRequest.FileSystemMount> toFilesystems(
            List<MetricRecordRequest.InstanceFilesystemPayload> filesystems) {
        return filesystems.stream()
                .map(fs -> new InstanceRealtimeMetricCreateRequest.FileSystemMount(
                        fs.getMountPoint(),
                        fs.getFsSizeBytes(),
                        fs.getFsAvailBytes()))
                .toList();
    }

    private static List<InstanceRealtimeMetricCreateRequest.NetworkInterfaceMetric> toNetworks(
            List<MetricRecordRequest.InstanceNetworkPayload> networks) {
        return networks.stream()
                .map(network -> new InstanceRealtimeMetricCreateRequest.NetworkInterfaceMetric(
                        network.getInterfaceName(),
                        network.getRxBytesTotal(),
                        network.getTxBytesTotal(),
                        network.getRxErrorsTotal(),
                        network.getTxErrorsTotal()))
                .toList();
    }
}