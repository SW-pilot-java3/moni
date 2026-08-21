package com.moni.api.domain.instance.sse;

import com.moni.api.domain.instance.dto.InstanceMetricStreamEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InstanceMetricStreamEventListener {

    private final InstanceMetricSseEmitterRegistry sseEmitterRegistry;

    @Async("sseTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMetricRecorded(InstanceMetricStreamEvent event) {
        sseEmitterRegistry.broadcast(event.instanceId(), event);
    }
}
