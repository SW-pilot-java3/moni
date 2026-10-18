package com.moni.api.domain.instance.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InstanceMetricSseEmitterRegistry {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByInstanceId = new ConcurrentHashMap<>();

    public SseEmitter register(Long instanceId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        List<SseEmitter> emitters = emittersByInstanceId.computeIfAbsent(instanceId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(instanceId, emitter));
        emitter.onTimeout(() -> remove(instanceId, emitter));
        emitter.onError(e -> remove(instanceId, emitter));

        sendInitEvent(instanceId, emitter);

        return emitter;
    }

    private void sendInitEvent(Long instanceId, SseEmitter emitter) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("connect")
                        .data("인스턴스(Instance) 실시간 SSE 스트림 연결 성공 (instanceId: " + instanceId + ")"));
            }
        } catch (Exception e) {
            log.warn("인스턴스 SSE 초기 연결 이벤트 전송 실패 (instanceId: {}, error: {})", instanceId, e.getMessage());
            remove(instanceId, emitter);
        }
    }

    public void broadcast(Long instanceId, Object data) {
        List<SseEmitter> emitters = emittersByInstanceId.get(instanceId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("instance_metric")
                        .data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE event to instance {}, removing emitter", instanceId, e);
                remove(instanceId, emitter);
            }
        }
    }

    private void remove(Long instanceId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByInstanceId.get(instanceId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
