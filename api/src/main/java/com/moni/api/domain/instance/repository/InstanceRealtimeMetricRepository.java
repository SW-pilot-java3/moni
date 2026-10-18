package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.InstanceRealtimeMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InstanceRealtimeMetricRepository extends JpaRepository<InstanceRealtimeMetric, Long> {

    List<InstanceRealtimeMetric> findAllByInstanceIdAndCollectedAtBetweenOrderByCollectedAtAsc(
            Long instanceId, LocalDateTime from, LocalDateTime to);

    List<InstanceRealtimeMetric> findAllByInstanceIdOrderByCollectedAtDesc(Long instanceId, Pageable pageable);

    Optional<InstanceRealtimeMetric> findFirstByInstanceIdOrderByCollectedAtDesc(Long instanceId);

    Optional<InstanceRealtimeMetric> findFirstByInstanceIdAndCollectedAtLessThanOrderByCollectedAtDesc(
            Long instanceId, LocalDateTime collectedAt);

    @Modifying
    @Query("DELETE FROM InstanceRealtimeMetric m WHERE m.collectedAt < :before")
    int deleteAllByCollectedAtBefore(@Param("before") LocalDateTime before);
}