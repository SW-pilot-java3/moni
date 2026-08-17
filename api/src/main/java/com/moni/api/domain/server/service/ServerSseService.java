package com.moni.api.domain.server.service;

import com.moni.api.domain.server.dto.response.ServerSseStreamResponse;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.SseEmitterRepository;
import com.moni.api.global.error.exception.CustomException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerSseService {

    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final String EVENT_SERVER_METRIC = "server_metric";
    private static final String EVENT_CONNECT = "connect";

    private final ServerRepository serverRepository;
    private final SseEmitterRepository sseEmitterRepository;

    public SseEmitter subscribe(Long serverId) {
        if (!serverRepository.existsById(serverId)) {
            throw new CustomException(ServerErrorCode.SERVER_NOT_FOUND);
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> {
            log.info("서버(Server) SSE 연결 정상 완료 (serverId: {})", serverId);
            sseEmitterRepository.delete(serverId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("서버(Server) SSE 연결 타임아웃 종료 (serverId: {})", serverId);
            sseEmitterRepository.delete(serverId, emitter);
        });

        emitter.onError((e) -> {
            log.warn("서버(Server) SSE 연결 오류 발생 (serverId: {}, error: {})", serverId, e.getMessage());
            sseEmitterRepository.delete(serverId, emitter);
        });

        sseEmitterRepository.save(serverId, emitter);

        sendInitEvent(serverId, emitter);

        return emitter;
    }

    public void broadcastServerMetric(Long serverId, ServerSseStreamResponse metric) {
        List<SseEmitter> emitters = sseEmitterRepository.findAllByServerId(serverId);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(EVENT_SERVER_METRIC)
                        .data(metric));
            } catch (IOException e) {
                log.warn("서버(Server) SSE 실시간 메트릭 전송 실패, 세션 제거 (serverId: {}, error: {})", serverId, e.getMessage());
                sseEmitterRepository.delete(serverId, emitter);
            }
        }
    }

    private void sendInitEvent(Long serverId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_CONNECT)
                    .data("서버(Server) 실시간 SSE 스트림 연결 성공 (serverId: " + serverId + ")"));
        } catch (IOException e) {
            log.warn("서버(Server) SSE 초기 연결 이벤트 전송 실패 (serverId: {})", serverId);
            sseEmitterRepository.delete(serverId, emitter);
        }
    }
}
