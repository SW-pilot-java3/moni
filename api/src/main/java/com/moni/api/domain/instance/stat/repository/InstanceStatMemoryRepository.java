package com.moni.api.domain.instance.stat.repository;

import com.moni.api.domain.instance.stat.entity.InstanceStatMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceStatMemoryRepository extends JpaRepository<InstanceStatMemory, Long> {

    List<InstanceStatMemory> findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long instanceId, String timeWindow, LocalDateTime from, LocalDateTime to);
}