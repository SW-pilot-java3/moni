package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceCpuMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceCpuMetricRepository extends JpaRepository<InstanceCpuMetric, Long> {
}