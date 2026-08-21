package com.moni.api.domain.server.stat.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.server.entity.JvmMetric;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerExecutorMetric;
import com.moni.api.domain.server.entity.ServerHikariCpPoolMetric;
import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import com.moni.api.domain.server.entity.ServerRealtimeMetric;
import com.moni.api.domain.server.entity.ServerStatus;
import com.moni.api.domain.server.repository.ServerRealtimeMetricRepository;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerStatBatchServiceTest {

    @InjectMocks
    private ServerStatBatchService serverStatBatchService;

    @Mock
    private ServerRepository serverRepository;

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

    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        Instance instance = Instance.builder().name("TestInstance").build();
        setEntityId(instance, 10L);

        server = Server.builder()
                .instance(instance)
                .name("TestServer")
                .port(8080)
                .status(ServerStatus.CONNECTED)
                .build();
        setEntityId(server, 1L);
    }

    private void setEntityId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Test
    @DisplayName("5분 단위 원시 메트릭 집계(5M) 성공 검증")
    void aggregateFiveMinuteForServer_Success() {
        // given
        LocalDateTime from = LocalDateTime.of(2026, 8, 21, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 21, 12, 5, 0);

        // 1차 수집 스냅샷 (12:00:00)
        ServerRealtimeMetric metric1 = ServerRealtimeMetric.builder()
                .serverId(1L)
                .collectedAt(from)
                .jvmMetric(JvmMetric.builder()
                        .jvmHeapUsedBytes(500_000_000L)
                        .jvmOldGenUsedBytes(200_000_000L)
                        .gcPauseSecondsCount(10L)
                        .gcPauseSecondsSum(1.0)
                        .jvmThreadsBlocked(2)
                        .build())
                .build();

        ServerHttpEndpointMetric ep1_200 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/users").method("GET").status("200")
                .requestsCount(100L).requestsSum(2.0).requestsMax(0.05).collectedAt(from).build();
        ServerHttpEndpointMetric ep1_500 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/users").method("GET").status("500")
                .requestsCount(10L).requestsSum(0.5).requestsMax(0.10).collectedAt(from).build();
        metric1.getHttpEndpoints().addAll(List.of(ep1_200, ep1_500));

        ServerHikariCpPoolMetric pool1 = ServerHikariCpPoolMetric.builder()
                .poolName("HikariPool-1").active(5).pending(1).timeoutsTotal(0L).collectedAt(from).build();
        metric1.getHikaricpPools().add(pool1);

        ServerExecutorMetric exec1 = ServerExecutorMetric.builder()
                .name("taskExecutor").active(4).max(10).queuedTasks(2).collectedAt(from).build();
        metric1.getExecutors().add(exec1);

        // 2차 수집 스냅샷 (12:05:00)
        ServerRealtimeMetric metric2 = ServerRealtimeMetric.builder()
                .serverId(1L)
                .collectedAt(to)
                .jvmMetric(JvmMetric.builder()
                        .jvmHeapUsedBytes(700_000_000L)
                        .jvmOldGenUsedBytes(300_000_000L)
                        .gcPauseSecondsCount(15L)
                        .gcPauseSecondsSum(1.5)
                        .jvmThreadsBlocked(4)
                        .build())
                .build();

        ServerHttpEndpointMetric ep2_200 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/users").method("GET").status("200")
                .requestsCount(190L).requestsSum(3.8).requestsMax(0.08).collectedAt(to).build();
        ServerHttpEndpointMetric ep2_500 = ServerHttpEndpointMetric.builder()
                .uri("/api/v1/users").method("GET").status("500")
                .requestsCount(20L).requestsSum(1.2).requestsMax(0.12).collectedAt(to).build();
        metric2.getHttpEndpoints().addAll(List.of(ep2_200, ep2_500));

        ServerHikariCpPoolMetric pool2 = ServerHikariCpPoolMetric.builder()
                .poolName("HikariPool-1").active(7).pending(2).timeoutsTotal(1L).collectedAt(to).build();
        metric2.getHikaricpPools().add(pool2);

        ServerExecutorMetric exec2 = ServerExecutorMetric.builder()
                .name("taskExecutor").active(6).max(10).queuedTasks(4).collectedAt(to).build();
        metric2.getExecutors().add(exec2);

        when(serverRealtimeMetricRepository.findAllByServerIdAndCollectedAtBetweenOrderByCollectedAtAsc(1L, from, to))
                .thenReturn(List.of(metric1, metric2));

        // when
        serverStatBatchService.aggregateFiveMinuteForServer(server, from, to);

        // then - JVM 5분 집계 검증
        ArgumentCaptor<StatJvm> jvmCaptor = ArgumentCaptor.forClass(StatJvm.class);
        verify(statJvmRepository).save(jvmCaptor.capture());
        StatJvm savedJvm = jvmCaptor.getValue();
        assertThat(savedJvm.getTimeWindow()).isEqualTo("5M");
        assertThat(savedJvm.getHeapUsedAvg()).isEqualTo(600_000_000L); // (500M + 700M) / 2
        assertThat(savedJvm.getHeapUsedMax()).isEqualTo(700_000_000L);
        assertThat(savedJvm.getGcPauseCountSum()).isEqualTo(5L); // 15 - 10
        assertThat(savedJvm.getGcPauseSecondsSum()).isCloseTo(0.5, offset(0.001)); // 1.5 - 1.0
        assertThat(savedJvm.getThreadBlockedMax()).isEqualTo(4);

        // then - HTTP 5분 집계 검증
        ArgumentCaptor<StatHttp> httpCaptor = ArgumentCaptor.forClass(StatHttp.class);
        verify(statHttpRepository).save(httpCaptor.capture());
        StatHttp savedHttp = httpCaptor.getValue();
        assertThat(savedHttp.getTimeWindow()).isEqualTo("5M");
        assertThat(savedHttp.getUri()).isEqualTo("/api/v1/users");
        assertThat(savedHttp.getMethod()).isEqualTo("GET");
        assertThat(savedHttp.getTotalRequestsCount()).isEqualTo(100L); // 200: (190-100)=90, 500: (20-10)=10 -> 100
        assertThat(savedHttp.getErrorCountSum()).isEqualTo(10L); // 500 status delta: 10
        assertThat(savedHttp.getErrorRateAvg()).isCloseTo(10.0, offset(0.001)); // 10 / 100 * 100% = 10.0%
        assertThat(savedHttp.getMaxResTimeMs()).isCloseTo(120.0, offset(0.001)); // 0.12s * 1000 = 120.0ms
        assertThat(savedHttp.getAvgResTimeMs()).isCloseTo(25.0, offset(0.001)); // ( (3.8-2.0) + (1.2-0.5) ) / 100 * 1000 = 2.5 / 100 * 1000 = 25.0ms

        // then - HikariCP 5분 집계 검증
        ArgumentCaptor<StatHikariCp> hikariCaptor = ArgumentCaptor.forClass(StatHikariCp.class);
        verify(statHikariCpRepository).save(hikariCaptor.capture());
        StatHikariCp savedHikari = hikariCaptor.getValue();
        assertThat(savedHikari.getTimeWindow()).isEqualTo("5M");
        assertThat(savedHikari.getPoolName()).isEqualTo("HikariPool-1");
        assertThat(savedHikari.getActivePoolAvg()).isCloseTo(6.0, offset(0.001)); // (5 + 7) / 2
        assertThat(savedHikari.getActivePoolMax()).isEqualTo(7);
        assertThat(savedHikari.getPendingThreadsMax()).isEqualTo(2);
        assertThat(savedHikari.getTimeoutCountSum()).isEqualTo(1); // 1 - 0

        // then - ThreadPool 5분 집계 검증
        ArgumentCaptor<StatThreadPool> threadPoolCaptor = ArgumentCaptor.forClass(StatThreadPool.class);
        verify(statThreadPoolRepository).save(threadPoolCaptor.capture());
        StatThreadPool savedThreadPool = threadPoolCaptor.getValue();
        assertThat(savedThreadPool.getTimeWindow()).isEqualTo("5M");
        assertThat(savedThreadPool.getName()).isEqualTo("taskExecutor");
        assertThat(savedThreadPool.getActiveThreadsAvg()).isCloseTo(5.0, offset(0.001)); // (4 + 6) / 2
        assertThat(savedThreadPool.getQueuedTasksAvg()).isCloseTo(3.0, offset(0.001)); // (2 + 4) / 2
        assertThat(savedThreadPool.getQueuedTasksMax()).isEqualTo(4);
    }

    @Test
    @DisplayName("1시간 단위 롤업 집계(1H) 가중치 기반 무결성 검증")
    void aggregateOneHourForServer_Success() {
        // given
        LocalDateTime from = LocalDateTime.of(2026, 8, 21, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 21, 13, 0, 0);

        // 5M HTTP 레코드 2건 준비:
        // 1번 5M 레코드: 요청 1건, 소요시간 0.01초(10ms), 에러 1건 (100% 에러율)
        StatHttp stat1 = StatHttp.builder()
                .server(server)
                .timeWindow("5M")
                .statTime(from.plusMinutes(5))
                .uri("/api/v1/orders")
                .method("POST")
                .totalRequestsCount(1L)
                .totalRequestsSum(0.01)
                .errorCountSum(1L)
                .errorRateAvg(100.0)
                .rpsAvg(0.0)
                .rpsMax(1.0)
                .avgResTimeMs(10.0)
                .maxResTimeMs(10.0)
                .build();

        // 2번 5M 레코드: 요청 99건, 소요시간 9.99초(100.9ms), 에러 0건 (0% 에러율)
        StatHttp stat2 = StatHttp.builder()
                .server(server)
                .timeWindow("5M")
                .statTime(from.plusMinutes(10))
                .uri("/api/v1/orders")
                .method("POST")
                .totalRequestsCount(99L)
                .totalRequestsSum(9.99)
                .errorCountSum(0L)
                .errorRateAvg(0.0)
                .rpsAvg(0.3)
                .rpsMax(5.0)
                .avgResTimeMs(100.9)
                .maxResTimeMs(200.0)
                .build();

        when(statHttpRepository.findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(1L, "5M", from.plusSeconds(1), to))
                .thenReturn(List.of(stat1, stat2));

        // when
        serverStatBatchService.aggregateOneHourForServer(server, from, to);

        // then - HTTP 1시간 롤업 집계 검증 (가중 평균 및 가중 에러율)
        ArgumentCaptor<StatHttp> httpCaptor = ArgumentCaptor.forClass(StatHttp.class);
        verify(statHttpRepository).save(httpCaptor.capture());
        StatHttp saved1H = httpCaptor.getValue();

        assertThat(saved1H.getTimeWindow()).isEqualTo("1H");
        assertThat(saved1H.getTotalRequestsCount()).isEqualTo(100L); // 1 + 99
        assertThat(saved1H.getErrorCountSum()).isEqualTo(1L); // 1 + 0

        // 단순 평균 (100% + 0%) / 2 = 50% 가 아닌 가중 에러율 1/100 * 100% = 1.0%
        assertThat(saved1H.getErrorRateAvg()).isCloseTo(1.0, offset(0.001));

        // 가중 평균 응답시간: (0.01 + 9.99) / 100 * 1000 = 100.0ms
        assertThat(saved1H.getAvgResTimeMs()).isCloseTo(100.0, offset(0.001));
        assertThat(saved1H.getMaxResTimeMs()).isCloseTo(200.0, offset(0.001));
        assertThat(saved1H.getRpsMax()).isCloseTo(5.0, offset(0.001));
    }

    @Test
    @DisplayName("원시 데이터가 비어있을 경우 집계를 건너뜀")
    void aggregateFiveMinuteForServer_EmptyData() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 21, 12, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 21, 12, 5, 0);

        when(serverRealtimeMetricRepository.findAllByServerIdAndCollectedAtBetweenOrderByCollectedAtAsc(1L, from, to))
                .thenReturn(Collections.emptyList());

        serverStatBatchService.aggregateFiveMinuteForServer(server, from, to);

        verify(statJvmRepository, never()).save(any());
        verify(statHttpRepository, never()).save(any());
        verify(statHikariCpRepository, never()).save(any());
        verify(statThreadPoolRepository, never()).save(any());
    }
}
