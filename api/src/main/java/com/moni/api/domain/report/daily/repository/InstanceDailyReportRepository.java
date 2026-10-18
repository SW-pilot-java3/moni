package com.moni.api.domain.report.daily.repository;

import com.moni.api.domain.report.daily.entity.InstanceDailyReport;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceDailyReportRepository extends JpaRepository<InstanceDailyReport, Long> {

    boolean existsByInstanceIdAndReportDate(Long instanceId, LocalDate reportDate);
}
