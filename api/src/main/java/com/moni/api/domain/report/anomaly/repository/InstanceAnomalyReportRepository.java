package com.moni.api.domain.report.anomaly.repository;

import com.moni.api.domain.report.anomaly.entity.InstanceAnomalyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceAnomalyReportRepository extends JpaRepository<InstanceAnomalyReport, Long> {
}