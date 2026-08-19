package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceFileSystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceFileSystemMetricRepository extends JpaRepository<InstanceFileSystemMetric, Long> {

    @Query("SELECT f FROM instance_filesystem_metrics f " +
            "WHERE f.realtimeMetric.instance.id = :instanceId " +
            "AND f.collectedAt BETWEEN :from AND :to " +
            "ORDER BY f.collectedAt ASC")
    List<InstanceFileSystemMetric> findAllByInstanceIdAndCollectedAtBetween(
            @Param("instanceId") Long instanceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}