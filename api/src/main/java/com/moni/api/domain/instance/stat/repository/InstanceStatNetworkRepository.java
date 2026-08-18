package com.moni.api.domain.instance.stat.repository;

import com.moni.api.domain.instance.stat.entity.InstanceStatNetwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InstanceStatNetworkRepository extends JpaRepository<InstanceStatNetwork, Long> {

    List<InstanceStatNetwork> findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long instanceId, String timeWindow, LocalDateTime from, LocalDateTime to);
}