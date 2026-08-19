package com.moni.api.domain.instance.stat.repository;

import com.moni.api.domain.instance.stat.entity.InstanceStatCpu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceStatCpuRepository extends JpaRepository<InstanceStatCpu, Long> {

    List<InstanceStatCpu> findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long instanceId, String timeWindow, LocalDateTime from, LocalDateTime to);
}