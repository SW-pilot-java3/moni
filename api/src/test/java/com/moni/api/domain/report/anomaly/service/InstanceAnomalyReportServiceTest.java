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

import com.moni.api.domain.instance.dto.InstanceMetricThresholdExceededEvent;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.enums.MetricKey;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.report.anomaly.dto.AnomalyReportContent;
import com.moni.api.domain.report.anomaly.entity.InstanceAnomalyReport;
import com.moni.api.domain.report.anomaly.repository.InstanceAnomalyReportRepository;
import com.moni.api.domain.report.client.ClaudeReportClient;
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
class InstanceAnomalyReportServiceTest {

    private InstanceAnomalyReportService instanceAnomalyReportService;

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private InstanceThresholdRepository instanceThresholdRepository;

    @Mock
    private InstanceAnomalyReportRepository instanceAnomalyReportRepository;

    @Mock
    private ClaudeReportClient claudeReportClient;

    @BeforeEach
    void setUp() {
        instanceAnomalyReportService = new InstanceAnomalyReportService(
                instanceRepository, instanceThresholdRepository, instanceAnomalyReportRepository,
                claudeReportClient, new ObjectMapper());
    }

    @Test
    @DisplayName("임계치 초과 이벤트를 받으면 Claude 분석 결과로 이상탐지 리포트를 저장한다")
    void generateReport_savesAnomalyReport() {
        // given
        Long instanceId = 1L;
        LocalDateTime collectedAt = LocalDateTime.now();
        InstanceMetricThresholdExceededEvent event = new InstanceMetricThresholdExceededEvent(
                instanceId, MetricKey.CPU_USAGE, 98.0, ThresholdSeverity.CRITICAL, collectedAt);

        Instance instance = Instance.builder().name("web-server-1").ip("10.0.0.1").build();
        InstanceThreshold threshold = InstanceThreshold.builder()
                .metricKey(MetricKey.CPU_USAGE).warningVal(80.0).criticalVal(95.0).build();
        AnomalyReportContent content = new AnomalyReportContent("CPU 급등", "트래픽 급증 추정", "오토스케일링 검토");

        given(instanceRepository.findById(instanceId)).willReturn(Optional.of(instance));
        given(instanceThresholdRepository.findByInstanceIdAndMetricKey(instanceId, MetricKey.CPU_USAGE))
                .willReturn(Optional.of(threshold));
        given(claudeReportClient.generate(
                eq("claude-haiku-4-5"), anyInt(), anyString(), anyString(), anyMap(), eq(AnomalyReportContent.class)))
                .willReturn(content);

        // when
        instanceAnomalyReportService.generateReport(event);

        // then
        ArgumentCaptor<InstanceAnomalyReport> captor = ArgumentCaptor.forClass(InstanceAnomalyReport.class);
        verify(instanceAnomalyReportRepository).save(captor.capture());

        InstanceAnomalyReport saved = captor.getValue();
        assertThat(saved.getInstance()).isEqualTo(instance);
        assertThat(saved.getMetricKey()).isEqualTo(MetricKey.CPU_USAGE);
        assertThat(saved.getSeverity()).isEqualTo(ThresholdSeverity.CRITICAL);
        assertThat(saved.getValue()).isEqualTo(98.0);
        assertThat(saved.getSummary()).isEqualTo("CPU 급등");
        assertThat(saved.getContent()).contains("트래픽 급증 추정", "오토스케일링 검토");
    }

    @Test
    @DisplayName("인스턴스가 존재하지 않으면 INSTANCE_NOT_FOUND 예외를 던진다")
    void generateReport_instanceNotFound_throwsException() {
        // given
        Long instanceId = 999L;
        InstanceMetricThresholdExceededEvent event = new InstanceMetricThresholdExceededEvent(
                instanceId, MetricKey.CPU_USAGE, 98.0, ThresholdSeverity.CRITICAL, LocalDateTime.now());
        given(instanceRepository.findById(instanceId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> instanceAnomalyReportService.generateReport(event))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(InstanceErrorCode.INSTANCE_NOT_FOUND));
        verify(instanceAnomalyReportRepository, never()).save(any());
    }
}
