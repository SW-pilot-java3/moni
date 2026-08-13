package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.dto.*;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.CommonErrorCode;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceThresholdRepository instanceThresholdRepository;
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

    @Transactional
    public InstanceUpdateResponse updateInstance(Long instanceId, InstanceUpdateRequest request) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        instance.update(request.name(), request.ip());

        return InstanceUpdateResponse.from(instance);
    }

    @Transactional
    public void deleteInstance(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

        instanceRepository.delete(instance);
    }

    @Transactional
    public InstanceThresholdUpdateResponse updateThresholds(Long instanceId, InstanceThresholdUpdateRequest request) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(InstanceErrorCode.INSTANCE_NOT_FOUND));

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

