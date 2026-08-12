package com.moni.api.domain.instance.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.global.error.ErrorCode;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceService {

    private final InstanceRepository instanceRepository;

    public Instance getInstanceById(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSTANCE_NOT_FOUND));
    }
}
