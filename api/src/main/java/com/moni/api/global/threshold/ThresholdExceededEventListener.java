package com.moni.api.global.threshold;

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.report.anomaly.service.InstanceAnomalyReportService;
import com.moni.api.domain.report.anomaly.service.ServerAnomalyReportService;
import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 디바운스·쿨다운 없이 임계치 초과마다 즉시 이상탐지 리포트를 생성한다 — 연속 3회(30초) 초과 시에만
 * 호출하는 디바운스와 재알림 쿨다운은 다음 작업으로 이 리스너에 추가될 예정이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdExceededEventListener {

    private final InstanceAnomalyReportService instanceAnomalyReportService;
    private final ServerAnomalyReportService serverAnomalyReportService;

    @Async("anomalyReportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstanceMetricThresholdExceeded(InstanceMetricThresholdExceededEvent event) {
        log.warn("[임계치 초과] instanceId={}, metricKey={}, value={}, severity={}, collectedAt={}",
                event.instanceId(), event.metricKey(), event.value(), event.severity(), event.collectedAt());
        try {
            instanceAnomalyReportService.generateReport(event);
        } catch (Exception e) {
            log.error("인스턴스 이상탐지 리포트 생성 실패 - instanceId={}, metricKey={}",
                    event.instanceId(), event.metricKey(), e);
        }
    }

    @Async("anomalyReportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServerMetricThresholdExceeded(ServerMetricThresholdExceededEvent event) {
        log.warn("[임계치 초과] serverId={}, metricKey={}, value={}, severity={}, collectedAt={}",
                event.serverId(), event.metricKey(), event.value(), event.severity(), event.collectedAt());
        try {
            serverAnomalyReportService.generateReport(event);
        } catch (Exception e) {
            log.error("서버 이상탐지 리포트 생성 실패 - serverId={}, metricKey={}",
                    event.serverId(), event.metricKey(), e);
        }
    }
}
