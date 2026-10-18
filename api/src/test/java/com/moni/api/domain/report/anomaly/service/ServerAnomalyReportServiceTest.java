package com.moni.api.domain.report.anomaly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.report.anomaly.dto.AnomalyReportContent;
import com.moni.api.domain.report.anomaly.entity.ServerAnomalyReport;
import com.moni.api.domain.report.anomaly.repository.ServerAnomalyReportRepository;
import com.moni.api.domain.report.client.ClaudeReportClient;
import com.moni.api.domain.server.dto.ServerMetricThresholdExceededEvent;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import com.moni.api.global.threshold.ThresholdSeverity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ServerAnomalyReportServiceTest {

    private ServerAnomalyReportService serverAnomalyReportService;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ServerThresholdRepository serverThresholdRepository;

    @Mock
    private ServerAnomalyReportRepository serverAnomalyReportRepository;

    @Mock
    private ClaudeReportClient claudeReportClient;

    @BeforeEach
    void setUp() {
        serverAnomalyReportService = new ServerAnomalyReportService(
                serverRepository, serverThresholdRepository, serverAnomalyReportRepository,
                claudeReportClient, new ObjectMapper());
    }

    @Test
    @DisplayName("임계치 초과 이벤트를 받으면 Claude 분석 결과로 이상탐지 리포트를 저장한다")
    void generateReport_savesAnomalyReport() {
        // given
        Long serverId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        ServerMetricThresholdExceededEvent event = new ServerMetricThresholdExceededEvent(
                serverId, ServerMetricKey.JVM_HEAP_USAGE, 92.0, ThresholdSeverity.CRITICAL, collectedAt);

        Server server = Server.builder().name("api-server-1").port(8080).build();
        ServerThreshold threshold = ServerThreshold.builder()
                .metricKey(ServerMetricKey.JVM_HEAP_USAGE).warningValue(80.0).criticalValue(90.0).build();
        AnomalyReportContent content = new AnomalyReportContent("Heap 사용률 급등", "메모리 누수 의심", "힙 덤프 분석 필요");

        given(serverRepository.findById(serverId)).willReturn(Optional.of(server));
        given(serverThresholdRepository.findByServerIdAndMetricKey(serverId, ServerMetricKey.JVM_HEAP_USAGE))
                .willReturn(Optional.of(threshold));
        given(claudeReportClient.generate(
                eq("claude-haiku-4-5"), anyInt(), anyString(), anyString(), anyMap(), eq(AnomalyReportContent.class)))
                .willReturn(content);

        // when
        serverAnomalyReportService.generateReport(event);

        // then
        ArgumentCaptor<ServerAnomalyReport> captor = ArgumentCaptor.forClass(ServerAnomalyReport.class);
        verify(serverAnomalyReportRepository).save(captor.capture());

        ServerAnomalyReport saved = captor.getValue();
        assertThat(saved.getServer()).isEqualTo(server);
        assertThat(saved.getMetricKey()).isEqualTo(ServerMetricKey.JVM_HEAP_USAGE);
        assertThat(saved.getSeverity()).isEqualTo(ThresholdSeverity.CRITICAL);
        assertThat(saved.getValue()).isEqualTo(92.0);
        assertThat(saved.getSummary()).isEqualTo("Heap 사용률 급등");
        assertThat(saved.getContent()).contains("메모리 누수 의심", "힙 덤프 분석 필요");
    }

    @Test
    @DisplayName("서버가 존재하지 않으면 SERVER_NOT_FOUND 예외를 던진다")
    void generateReport_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        ServerMetricThresholdExceededEvent event = new ServerMetricThresholdExceededEvent(
                serverId, ServerMetricKey.JVM_HEAP_USAGE, 92.0, ThresholdSeverity.CRITICAL, LocalDateTime.now());
        given(serverRepository.findById(serverId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverAnomalyReportService.generateReport(event))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ServerErrorCode.SERVER_NOT_FOUND));
        verify(serverAnomalyReportRepository, never()).save(any());
    }
}
