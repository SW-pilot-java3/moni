package com.moni.api.domain.instance.stat.repository;

import com.moni.api.domain.instance.stat.entity.InstanceStatDisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceStatDiskRepository extends JpaRepository<InstanceStatDisk, Long> {

    List<InstanceStatDisk> findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long instanceId, String timeWindow, LocalDateTime from, LocalDateTime to);
}