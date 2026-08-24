package com.moni.api.domain.report.anomaly.repository;

import com.moni.api.domain.report.anomaly.entity.ServerAnomalyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerAnomalyReportRepository extends JpaRepository<ServerAnomalyReport, Long> {
}