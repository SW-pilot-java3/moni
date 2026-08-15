package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRealtimeMetricRepository extends JpaRepository<ServerRealtimeMetric, Long> {

    @EntityGraph(attributePaths = {"httpEndpoints", "hikaricpPools", "executors"})
    List<ServerRealtimeMetric> findByServerIdOrderByCollectedAtDesc(Long serverId, Pageable pageable);
}
