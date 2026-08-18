package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.dto.*;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.CommonErrorCode;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InstanceService {

    private static final int DEFAULT_REALTIME_METRIC_LIMIT = 30;

    private final InstanceRepository instanceRepository;
    private final InstanceThresholdRepository instanceThresholdRepository;
    private final InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;
    private final UserRepository userRepository;

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

    private Instance getInstanceOwnedBy(Long instanceId, Long userId) {
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

        List<InstanceRealtimeMetricResponse> responses = new ArrayList<>();
        InstanceRealtimeMetric previous = null;
        for (InstanceRealtimeMetric current : metrics) {
            Double cpuUsagePct = previous == null
                    ? null
                    : CpuUsageCalculator.calculate(previous.getCpuMetrics(), current.getCpuMetrics());
            responses.add(new InstanceRealtimeMetricResponse(
                    current.getCollectedAt(),
                    cpuUsagePct,
                    current.getMemoryMetrics().getMemAvailableBytes()
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
}

