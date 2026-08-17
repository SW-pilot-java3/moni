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
        emittersMap.computeIfAbsent(serverId, key -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void delete(Long serverId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersMap.get(serverId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersMap.remove(serverId);
            }
        }
    }

    public List<SseEmitter> findAllByServerId(Long serverId) {
        List<SseEmitter> emitters = emittersMap.get(serverId);
        if (emitters == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(emitters);
    }

    public void deleteAllByServerId(Long serverId) {
        emittersMap.remove(serverId);
    }
}
