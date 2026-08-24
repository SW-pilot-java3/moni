package com.moni.api.domain.report.anomaly.repository;

import com.moni.api.domain.report.anomaly.entity.ServerAnomalyReport;
import com.moni.api.domain.server.entity.ServerMetricKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerAnomalyReportRepository extends JpaRepository<ServerAnomalyReport, Long> {

    Optional<ServerAnomalyReport> findFirstByServerIdAndMetricKeyOrderByCollectedAtDesc(
            Long serverId, ServerMetricKey metricKey);
}
