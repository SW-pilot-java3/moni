package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.server.dto.response.ServerHistoryMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerRealtimeMetricsResponse;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.validator.ServerValidator;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ServerMetricServiceTest {

    @InjectMocks
    private ServerMetricService serverMetricService;

    @Mock
    private ServerValidator serverValidator;

    @Mock
    private ServerRealtimeMetricRepository serverRealtimeMetricRepository;

    @Mock
    private StatJvmRepository statJvmRepository;

    @Mock
    private StatHttpRepository statHttpRepository;

    @Mock
    private StatHikariCpRepository statHikariCpRepository;

    @Mock
    private StatThreadPoolRepository statThreadPoolRepository;

    private final Long userId = 1L;

    @Test
    @DisplayName("서버 실시간 메트릭 조회 성공")
    void getServerRealtimeMetrics_success() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);
        LocalDateTime now = LocalDateTime.now();

        JvmMetric jvm1 = JvmMetric.builder()
                .jvmHeapUsedBytes(67108864L)
                .jvmHeapMaxBytes(1610612736L)
                .jvmOldGenUsedBytes(33554432L)
                .gcPauseSecondsSum(0.01)
                .processUptimeSeconds(12345.0)
                .build();

        ServerHttpEndpointMetric http1 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/servers")
                .method("POST")
                .status("201")
                .requestsCount(100L)
                .requestsSum(2.5)
                .requestsMax(0.05)
                .build();

        ServerHikariCpPoolMetric hikari1 = ServerHikariCpPoolMetric.builder()
                .poolName("HikariPool-1")
                .active(5)
                .idle(5)
                .max(10)
                .build();

        ServerExecutorMetric exec1 = ServerExecutorMetric.builder()
                .name("appExecutor")
                .active(3)
                .max(10)
                .queuedTasks(1)
                .queueRemaining(99)
                .build();

        ServerRealtimeMetric metric1 = ServerRealtimeMetric.builder()
                .id(1L)
                .serverId(serverId)
                .collectedAt(now.minusSeconds(10))
                .jvmMetric(jvm1)
                .httpEndpoints(List.of(http1))
                .hikaricpPools(List.of(hikari1))
                .executors(List.of(exec1))
                .build();

        ServerRealtimeMetric metric2 = ServerRealtimeMetric.builder()
                .id(2L)
                .serverId(serverId)
                .collectedAt(now)
                .jvmMetric(jvm1)
                .httpEndpoints(List.of(http1))
                .hikaricpPools(List.of(hikari1))
                .executors(List.of(exec1))
                .build();

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(mockServer);
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(serverId), any(Pageable.class)))
                .willReturn(List.of(metric2, metric1));

        // when
        ServerRealtimeMetricsResponse response = serverMetricService.getServerRealtimeMetrics(serverId, userId, 30);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrent()).isNotNull();
        assertThat(response.getCurrent().getCollectedAt()).isEqualTo(now);
        assertThat(response.getCurrent().getProcessUptimeSeconds()).isEqualTo(12345.0);
        assertThat(response.getCurrent().getHttpEndpoints()).hasSize(1);

        assertThat(response.getSeries()).hasSize(2);
        assertThat(response.getSeries().get(0).getCollectedAt()).isEqualTo(now.minusSeconds(10));
        assertThat(response.getSeries().get(1).getCollectedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 서버의 실시간 메트릭 조회 시 예외 발생")
    void getServerRealtimeMetrics_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        given(serverValidator.validateAndGetServer(serverId, userId))
                .willThrow(new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverMetricService.getServerRealtimeMetrics(serverId, userId, 30))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("수납 메트릭 데이터가 없을 경우 empty response 반환")
    void getServerRealtimeMetrics_emptyMetrics_returnsEmptyResponse() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);
        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(mockServer);
        given(serverRealtimeMetricRepository.findByServerIdOrderByCollectedAtDesc(eq(serverId), any(Pageable.class)))
                .willReturn(Collections.emptyList());

        // when
        ServerRealtimeMetricsResponse response = serverMetricService.getServerRealtimeMetrics(serverId, userId, 30);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrent()).isNull();
        assertThat(response.getSeries()).isEmpty();
    }

    @Test
    @DisplayName("어제 이전의 과거 시계열 및 요약 통계 조회 성공")
    void getServerHistoryMetrics_success() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime statTime = yesterday.atTime(14, 0);

        StatJvm statJvm = StatJvm.builder()
                .timeWindow("1H")
                .statTime(statTime)
                .heapUsedAvg(500_000_000L)
                .heapUsedMax(1_000_000_000L)
                .oldGenUsedAvg(200_000_000L)
                .gcPauseCountSum(2L)
                .gcPauseSecondsSum(0.05)
                .threadBlockedMax(1)
                .build();

        StatHttp statHttp = StatHttp.builder()
                .timeWindow("1H")
                .statTime(statTime)
                .uri("/api/payments")
                .method("POST")
                .totalRequestsCount(1000L)
                .rpsAvg(50.0)
                .rpsMax(100.0)
                .avgResTimeMs(120.0)
                .maxResTimeMs(500.0)
                .errorRateAvg(0.01)
                .build();

        StatHikariCp statHikari = StatHikariCp.builder()
                .timeWindow("1H")
                .statTime(statTime)
                .poolName("HikariPool-1")
                .activePoolAvg(4.5)
                .activePoolMax(10)
                .pendingThreadsMax(0)
                .timeoutCountSum(0)
                .build();

        StatThreadPool statThreadPool = StatThreadPool.builder()
                .timeWindow("1H")
                .statTime(statTime)
                .name("taskExecutor")
                .activeThreadsAvg(3.0)
                .maxThreadsAvg(10.0)
                .queuedTasksAvg(1.5)
                .queuedTasksMax(5)
                .build();

        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(mockServer);
        given(statJvmRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(List.of(statJvm));
        given(statHttpRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(List.of(statHttp));
        given(statHikariCpRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(List.of(statHikari));
        given(statThreadPoolRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(List.of(statThreadPool));

        // when
        ServerHistoryMetricsResponse response = serverMetricService.getServerHistoryMetrics(serverId, userId, yesterday);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getDate()).isEqualTo(yesterday);

        // summary 요약 검증
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getSummary().getJvm().getHeapUsedMaxBytes()).isEqualTo(1_000_000_000L);
        assertThat(response.getSummary().getHttpEndpoints()).hasSize(1);
        assertThat(response.getSummary().getHttpEndpoints().get(0).getUri()).isEqualTo("/api/payments");
        assertThat(response.getSummary().getHikaricpPools()).hasSize(1);
        assertThat(response.getSummary().getExecutors()).hasSize(1);

        // series 시계열 검증
        assertThat(response.getSeries()).hasSize(1);
        assertThat(response.getSeries().get(0).getStatTime()).isEqualTo(statTime);
        assertThat(response.getSeries().get(0).getJvmHeapUsedBytes()).isEqualTo(500_000_000L);
        assertThat(response.getSeries().get(0).getTotalRpsAvg()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("요청 수 가중 평균 기반 평균 응답시간 및 에러율 계산 검증")
    void getServerHistoryMetrics_weightedAverageCalculation() {
        // given
        Long serverId = 100L;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime time1 = yesterday.atTime(10, 0);
        LocalDateTime time2 = yesterday.atTime(11, 0);

        // 10시 데이터: 10,000건, 10ms, 에러율 0.0
        StatHttp http1 = StatHttp.builder()
                .timeWindow("1H")
                .statTime(time1)
                .uri("/api/test")
                .method("GET")
                .totalRequestsCount(10_000L)
                .rpsAvg(100.0)
                .rpsMax(200.0)
                .avgResTimeMs(10.0)
                .maxResTimeMs(50.0)
                .errorRateAvg(0.0)
                .build();

        // 11시 데이터: 1건, 1000ms, 에러율 1.0 (100%)
        StatHttp http2 = StatHttp.builder()
                .timeWindow("1H")
                .statTime(time2)
                .uri("/api/test")
                .method("GET")
                .totalRequestsCount(1L)
                .rpsAvg(0.0)
                .rpsMax(1.0)
                .avgResTimeMs(1000.0)
                .maxResTimeMs(1000.0)
                .errorRateAvg(1.0)
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);
        given(statJvmRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(Collections.emptyList());
        given(statHttpRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(List.of(http1, http2));
        given(statHikariCpRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(Collections.emptyList());
        given(statThreadPoolRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(eq(serverId), eq("1H"), any(), any()))
                .willReturn(Collections.emptyList());

        // when
        ServerHistoryMetricsResponse response = serverMetricService.getServerHistoryMetrics(serverId, yesterday);

        // then - 요약 통계 가중 평균 검증: (10ms * 10000건 + 1000ms * 1건) / 10001건 ≈ 10.1ms
        assertThat(response.getSummary().getHttpEndpoints()).hasSize(1);
        var epSummary = response.getSummary().getHttpEndpoints().get(0);
        assertThat(epSummary.getTotalRequestsCount()).isEqualTo(10_001L);
        assertThat(epSummary.getAvgResTimeMs()).isEqualTo(10.1);
        // 에러율 가중 평균: (0.0 * 10000 + 1.0 * 1) / 10001 ≈ 0.0000999 -> 0.0
        assertThat(epSummary.getErrorRateAvg()).isEqualTo(0.0);

        // then - 시간대별 시계열 가중 평균 검증
        assertThat(response.getSeries()).hasSize(2);
        assertThat(response.getSeries().get(0).getAvgLatencyMs()).isEqualTo(10.0);
        assertThat(response.getSeries().get(1).getAvgLatencyMs()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("오늘 날짜 요청 시 INVALID_HISTORICAL_DATE 예외 발생")
    void getServerHistoryMetrics_todayDate_throwsException() {
        // given
        Long serverId = 100L;
        Server mockServer = Mockito.mock(Server.class);
        LocalDate today = LocalDate.now();
        given(serverValidator.validateAndGetServer(serverId, userId)).willReturn(mockServer);

        // when & then
        assertThatThrownBy(() -> serverMetricService.getServerHistoryMetrics(serverId, userId, today))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.INVALID_HISTORICAL_DATE);
    }

    @Test
    @DisplayName("존재하지 않거나 권한 없는 서버의 과거 통계 조회 시 SERVER_NOT_FOUND 예외 발생")
    void getServerHistoryMetrics_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        given(serverValidator.validateAndGetServer(serverId, userId))
                .willThrow(new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> serverMetricService.getServerHistoryMetrics(serverId, userId, LocalDate.now().minusDays(1)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }
}
