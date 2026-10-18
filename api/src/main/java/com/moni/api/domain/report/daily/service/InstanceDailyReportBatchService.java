package com.moni.api.domain.report.daily.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.repository.InstanceRepository;
import com.moni.api.domain.instance.repository.InstanceThresholdRepository;
import com.moni.api.domain.instance.stat.entity.InstanceStatCpu;
import com.moni.api.domain.instance.stat.entity.InstanceStatDisk;
import com.moni.api.domain.instance.stat.entity.InstanceStatMemory;
import com.moni.api.domain.instance.stat.entity.InstanceStatNetwork;
import com.moni.api.domain.instance.stat.repository.InstanceStatCpuRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatDiskRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatMemoryRepository;
import com.moni.api.domain.instance.stat.repository.InstanceStatNetworkRepository;
import com.moni.api.domain.report.client.ClaudeReportClient;
import com.moni.api.domain.report.daily.dto.DailyReportContent;
import com.moni.api.domain.report.daily.entity.InstanceDailyReport;
import com.moni.api.domain.report.daily.repository.InstanceDailyReportRepository;
import com.moni.api.domain.report.exception.ReportErrorCode;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 인스턴스별 전날 5분 통계를 시간별로 재집계해 Claude Sonnet 5로 일별 리포트를 생성·저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceDailyReportBatchService {

    private static final String TIME_WINDOW = "5M";
    private static final String CLAUDE_MODEL = "claude-sonnet-5";
    private static final int MAX_TOKENS = 2000;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "summary", Map.of("type", "string"),
                    "trend_analysis", Map.of("type", "string"),
                    "notable_events", Map.of("type", "array", "items", Map.of("type", "string")),
                    "recommendation", Map.of("type", "string")
            ),
            "required", List.of("summary", "trend_analysis", "notable_events", "recommendation"),
            "additionalProperties", false
    );

    private final InstanceRepository instanceRepository;
    private final InstanceThresholdRepository instanceThresholdRepository;
    private final InstanceStatCpuRepository instanceStatCpuRepository;
    private final InstanceStatMemoryRepository instanceStatMemoryRepository;
    private final InstanceStatDiskRepository instanceStatDiskRepository;
    private final InstanceStatNetworkRepository instanceStatNetworkRepository;
    private final InstanceDailyReportRepository instanceDailyReportRepository;
    private final ClaudeReportClient claudeReportClient;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 10 0 * * *")
    public void generateDailyReports() {
        LocalDate reportDate = LocalDate.now().minusDays(1);

        for (Instance instance : instanceRepository.findAll()) {
            try {
                generateReport(instance, reportDate);
            } catch (Exception e) {
                log.error("일별 리포트 생성 실패 - instanceId={}, date={}", instance.getId(), reportDate, e);
            }
        }
    }

    @Transactional
    public void generateReport(Instance instance, LocalDate reportDate) {
        if (instanceDailyReportRepository.existsByInstanceIdAndReportDate(instance.getId(), reportDate)) {
            return;
        }

        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();
        Long instanceId = instance.getId();

        List<InstanceStatCpu> cpuStats = instanceStatCpuRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW, from, to);
        if (cpuStats.isEmpty()) {
            return;
        }
        List<InstanceStatMemory> memoryStats = instanceStatMemoryRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW, from, to);
        List<InstanceStatDisk> diskStats = instanceStatDiskRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW, from, to);
        List<InstanceStatNetwork> networkStats = instanceStatNetworkRepository
                .findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(instanceId, TIME_WINDOW, from, to);
        List<InstanceThreshold> thresholds = instanceThresholdRepository.findAllByInstanceId(instanceId);

        String userPrompt = DailyReportPromptBuilder.buildUserPrompt(
                instance, reportDate, cpuStats, memoryStats, diskStats, networkStats, thresholds);

        DailyReportContent content = claudeReportClient.generate(
                CLAUDE_MODEL, MAX_TOKENS, DailyReportPromptBuilder.SYSTEM_PROMPT, userPrompt,
                RESPONSE_SCHEMA, DailyReportContent.class);

        InstanceDailyReport report = InstanceDailyReport.builder()
                .instance(instance)
                .reportDate(reportDate)
                .summary(content.summary())
                .content(toJson(content))
                .build();

        try {
            instanceDailyReportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            log.info("이미 생성된 일별 리포트 - instanceId={}, date={}", instanceId, reportDate);
        }
    }

    private String toJson(DailyReportContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JacksonException e) {
            log.error("일별 리포트 JSON 직렬화 실패", e);
            throw new CustomException(ReportErrorCode.CLAUDE_RESPONSE_PARSE_FAILED);
        }
    }
}
