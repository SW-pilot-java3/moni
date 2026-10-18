package com.moni.api.domain.report.daily.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.entity.InstanceThreshold;
import com.moni.api.domain.instance.stat.entity.InstanceStatCpu;
import com.moni.api.domain.instance.stat.entity.InstanceStatDisk;
import com.moni.api.domain.instance.stat.entity.InstanceStatMemory;
import com.moni.api.domain.instance.stat.entity.InstanceStatNetwork;
import java.time.LocalDate;
import java.util.List;

/**
 * 5분 통계(stat_*) 리스트를 시간별(0~23시) 버킷으로 재집계해 Claude에 넘길 프롬프트 텍스트를 만든다.
 * 각 시간대는 HourlyStatAggregator의 규칙(avg는 평균의 평균, max/min은 대표값, sum은 합계)을 따른다.
 */
final class DailyReportPromptBuilder {

    private DailyReportPromptBuilder() {
    }

    static final String SYSTEM_PROMPT = """
            당신은 인프라 모니터링 시스템의 데이터 분석가입니다.
            주어진 인스턴스의 하루치 시간대별 통계와 임계치 설정을 보고,
            하루 동안의 추세와 특이 구간을 한국어로 분석해 지정된 JSON 스키마로만 응답하세요.
            원본 수치를 근거로 삼되, 추측이나 근거 없는 단정은 하지 마세요.
            """;

    static String buildUserPrompt(Instance instance, LocalDate reportDate,
                                   List<InstanceStatCpu> cpuStats,
                                   List<InstanceStatMemory> memoryStats,
                                   List<InstanceStatDisk> diskStats,
                                   List<InstanceStatNetwork> networkStats,
                                   List<InstanceThreshold> thresholds) {
        StringBuilder sb = new StringBuilder();
        sb.append("인스턴스: ").append(instance.getName()).append(" (id=").append(instance.getId()).append(")\n");
        sb.append("날짜: ").append(reportDate).append("\n\n");

        sb.append("[임계치 설정]\n");
        if (thresholds.isEmpty()) {
            sb.append("설정된 임계치 없음\n");
        } else {
            for (InstanceThreshold threshold : thresholds) {
                sb.append("- ").append(threshold.getMetricKey())
                        .append(": warning=").append(threshold.getWarningVal())
                        .append(", critical=").append(threshold.getCriticalVal())
                        .append("\n");
            }
        }

        sb.append("\n[시간별 통계]\n");
        sb.append("(5분 통계를 시간 단위로 재집계함 — 평균은 평균들의 평균, 최댓값/최솟값은 해당 시간대 대표값, 에러는 합계)\n");

        for (int hour = 0; hour < 24; hour++) {
            String line = buildHourLine(hour, cpuStats, memoryStats, diskStats, networkStats);
            if (line != null) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    private static String buildHourLine(int hour,
                                         List<InstanceStatCpu> cpuStats,
                                         List<InstanceStatMemory> memoryStats,
                                         List<InstanceStatDisk> diskStats,
                                         List<InstanceStatNetwork> networkStats) {
        List<InstanceStatCpu> cpuHour = cpuStats.stream().filter(s -> s.getStatTime().getHour() == hour).toList();
        if (cpuHour.isEmpty()) {
            return null;
        }
        List<InstanceStatMemory> memHour = memoryStats.stream().filter(s -> s.getStatTime().getHour() == hour).toList();
        List<InstanceStatDisk> diskHour = diskStats.stream().filter(s -> s.getStatTime().getHour() == hour).toList();
        List<InstanceStatNetwork> netHour = networkStats.stream().filter(s -> s.getStatTime().getHour() == hour).toList();

        Double cpuAvg = HourlyStatAggregator.avgOfAvg(cpuHour.stream().map(InstanceStatCpu::getCpuUsageAvg).toList());
        Double cpuMax = HourlyStatAggregator.maxOfMax(cpuHour.stream().map(InstanceStatCpu::getCpuUsageMax).toList());
        Long memAvgAvail = HourlyStatAggregator.avgOfAvgLong(memHour.stream().map(InstanceStatMemory::getMemAvailableAvg).toList());
        Long memMinAvail = HourlyStatAggregator.minOfMin(memHour.stream().map(InstanceStatMemory::getMemAvailableMin).toList());
        Double diskUtilMax = HourlyStatAggregator.maxOfMax(diskHour.stream().map(InstanceStatDisk::getDiskUtilMax).toList());
        Integer netErrors = HourlyStatAggregator.sum(netHour.stream().map(InstanceStatNetwork::getErrorsSum).toList());

        return String.format(
                "%02d시 - CPU 평균 %s%% 최대 %s%% | 가용메모리 평균 %s 최소 %s | 디스크 활용률 최대 %s%% | 네트워크 에러 %d건",
                hour,
                format(cpuAvg), format(cpuMax),
                formatBytes(memAvgAvail), formatBytes(memMinAvail),
                format(diskUtilMax),
                netErrors == null ? 0 : netErrors);
    }

    private static String format(Double value) {
        return value == null ? "N/A" : String.format("%.1f", value);
    }

    private static String formatBytes(Long bytes) {
        if (bytes == null) {
            return "N/A";
        }
        return String.format("%.1fGB", bytes / 1_073_741_824.0);
    }
}
