package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceDiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceDiskMetricsRepository extends JpaRepository<InstanceDiskMetric, Long> {

    @Query("SELECT d FROM instance_disk_metrics d " +
            "WHERE d.realtimeMetric.instance.id = :instanceId " +
            "AND d.collectedAt BETWEEN :from AND :to " +
            "ORDER BY d.collectedAt ASC")
    List<InstanceDiskMetric> findAllByInstanceIdAndCollectedAtBetween(
            @Param("instanceId") Long instanceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
