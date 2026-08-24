package com.moni.api.domain.report.anomaly.repository;

import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.report.anomaly.entity.InstanceAnomalyReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceAnomalyReportRepository extends JpaRepository<InstanceAnomalyReport, Long> {

    Optional<InstanceAnomalyReport> findFirstByInstanceIdAndMetricKeyOrderByCollectedAtDesc(
            Long instanceId, MetricKey metricKey);
}
