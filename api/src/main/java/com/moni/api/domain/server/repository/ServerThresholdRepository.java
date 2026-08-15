package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerThresholdRepository extends JpaRepository<ServerThreshold, Long> {

    List<ServerThreshold> findByServerId(Long serverId);

    Optional<ServerThreshold> findByServerIdAndMetricKey(Long serverId, ServerMetricKey metricKey);
}
