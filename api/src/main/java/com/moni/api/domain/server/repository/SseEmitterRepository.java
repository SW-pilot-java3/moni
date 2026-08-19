package com.moni.api.domain.server.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

    private final Map<Long, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    public void save(Long serverId, SseEmitter emitter) {
        emittersMap.compute(serverId, (key, emitters) -> {
            if (emitters == null) {
                emitters = new CopyOnWriteArrayList<>();
            }
            emitters.add(emitter);
            return emitters;
        });
    }

    public void delete(Long serverId, SseEmitter emitter) {
        emittersMap.computeIfPresent(serverId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    public List<SseEmitter> findAllByServerId(Long serverId) {
        List<SseEmitter> emitters = emittersMap.get(serverId);
        if (emitters == null) {
            return List.of();
        }
        return new ArrayList<>(emitters);
    }

    public void deleteAllByServerId(Long serverId) {
        emittersMap.remove(serverId);
    }
}
