package com.moni.api.global.threshold;

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ThresholdExceededEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstanceMetricThresholdExceeded(InstanceMetricThresholdExceededEvent event) {
        log.warn("[임계치 초과] instanceId={}, metricKey={}, value={}, severity={}, collectedAt={}",
                event.instanceId(), event.metricKey(), event.value(), event.severity(), event.collectedAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServerMetricThresholdExceeded(ServerMetricThresholdExceededEvent event) {
        log.warn("[임계치 초과] serverId={}, metricKey={}, value={}, severity={}, collectedAt={}",
                event.serverId(), event.metricKey(), event.value(), event.severity(), event.collectedAt());
    }
}
