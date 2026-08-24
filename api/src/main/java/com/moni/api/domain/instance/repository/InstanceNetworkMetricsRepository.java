package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceNetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceNetworkMetricsRepository extends JpaRepository<InstanceNetworkMetric, Long> {

    @Query("SELECT n FROM instance_network_metrics n " +
            "WHERE n.realtimeMetric.instance.id = :instanceId " +
            "AND n.collectedAt BETWEEN :from AND :to " +
            "ORDER BY n.collectedAt ASC")
    List<InstanceNetworkMetric> findAllByInstanceIdAndCollectedAtBetween(
            @Param("instanceId") Long instanceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<InstanceNetworkMetric> findAllByRealtimeMetricId(Long realtimeMetricId);

    List<InstanceNetworkMetric> findAllByRealtimeMetricIdIn(List<Long> realtimeMetricIds);
}