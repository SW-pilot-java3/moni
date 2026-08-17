package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.server.dto.response.ServerSseStreamResponse;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.SseEmitterRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ServerSseServiceTest {

    @InjectMocks
    private ServerSseService serverSseService;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Test
    @DisplayName("SSE 구독 성공 - 서버 존재 시 SseEmitter 생성 및 연결 저장소 등록")
    void subscribe_Success() {
        // given
        Long serverId = 1L;
        given(serverRepository.existsById(serverId)).willReturn(true);

        // when
        SseEmitter emitter = serverSseService.subscribe(serverId);

        // then
        assertThat(emitter).isNotNull();
        verify(serverRepository).existsById(serverId);
        verify(sseEmitterRepository).save(eq(serverId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("SSE 구독 실패 - 존재하지 않는 서버 ID 요청 시 SERVER_NOT_FOUND 예외 검증")
    void subscribe_ServerNotFound() {
        // given
        Long serverId = 999L;
        given(serverRepository.existsById(serverId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverSseService.subscribe(serverId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ServerErrorCode.SERVER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("서버 실시간 메트릭 브로드캐스팅 성공 - 구독자 Emitter 목록으로 이벤트 푸시")
    void broadcastServerMetric_Success() {
        // given
        Long serverId = 1L;
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        given(sseEmitterRepository.findAllByServerId(serverId)).willReturn(List.of(mockEmitter));

        ServerSseStreamResponse responsePayload = ServerSseStreamResponse.builder()
                .serverId(serverId)
                .collectedAt(LocalDateTime.now())
                .summary(ServerSseStreamResponse.ServerMetricSummaryDto.builder()
                        .totalRps(120.0)
                        .avgLatencyMs(35.0)
                        .build())
                .build();

        // when & then
        serverSseService.broadcastServerMetric(serverId, responsePayload);

        verify(sseEmitterRepository).findAllByServerId(serverId);
    }
}
