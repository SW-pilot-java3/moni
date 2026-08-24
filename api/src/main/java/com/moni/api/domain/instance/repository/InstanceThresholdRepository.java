package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstanceThresholdRepository extends JpaRepository<InstanceThreshold, Long> {

    List<InstanceThreshold> findAllByInstanceId(Long instanceId);

    Optional<InstanceThreshold> findByInstanceIdAndMetricKey(Long instanceId, MetricKey metricKey);
}
