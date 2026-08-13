package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstanceThresholdRepository extends JpaRepository<InstanceThreshold, Long> {

    List<InstanceThreshold> findAllByInstanceId(Long instanceId);
}
