package com.moni.api.domain.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.instance.dto.InstanceHistoryMetricsResponse;
import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.exception.InstanceErrorCode;
import com.moni.api.domain.instance.repository.InstanceRealtimeMetricRepository;
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
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstanceServiceTest {

    @InjectMocks
    private InstanceService instanceService;

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private InstanceThresholdRepository instanceThresholdRepository;

    @Mock
    private InstanceRealtimeMetricRepository instanceRealtimeMetricRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private InstanceStatCpuRepository instanceStatCpuRepository;

    @Mock
    private InstanceStatMemoryRepository instanceStatMemoryRepository;

    @Mock
    private InstanceStatDiskRepository instanceStatDiskRepository;

    @Mock
    private InstanceStatNetworkRepository instanceStatNetworkRepository;

    private final Long userId = 1L;
    private final Long instanceId = 100L;

    private Instance ownedInstance() {
        User user = User.builder().email("test@example.com").passwordHash("pass1234").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Instance instance = Instance.builder().name("instance1").ip("127.0.0.1").user(user).build();
        ReflectionTestUtils.setField(instance, "id", instanceId);
        return instance;
    }

    @Test
    @DisplayName("어제 이전의 과거 시계열 및 요약 통계 조회 성공")
    void getInstanceHistoryMetrics_success() {
        // given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime statTime = yesterday.atTime(14, 0);
        Instance instance = ownedInstance();

        InstanceStatCpu cpuStat = InstanceStatCpu.builder()
                .instance(instance).timeWindow("1H").statTime(statTime)
                .cpuUsageAvg(45.0).cpuUsageMax(80.0).cpuIowaitAvg(1.5)
                .build();
        InstanceStatMemory memoryStat = InstanceStatMemory.builder()
                .instance(instance).timeWindow("1H").statTime(statTime)
                .memAvailableAvg(2_000_000_000L).memAvailableMin(1_500_000_000L).swapUsedMax(0.0)
                .build();
        InstanceStatDisk diskStat = InstanceStatDisk.builder()
                .instance(instance).timeWindow("1H").statTime(statTime)
                .readIopsAvg(10.0).writeIopsAvg(5.0).diskUtilMax(30.0).diskUsedPctMax(60.0)
                .build();
        InstanceStatNetwork networkStat = InstanceStatNetwork.builder()
                .instance(instance).timeWindow("1H").statTime(statTime)
                .rxMbpsAvg(12.0).txMbpsAvg(3.0).errorsSum(0)
                .build();

        given(instanceRepository.findById(instanceId)).willReturn(Optional.of(instance));
        given(instanceStatCpuRepository.findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                eq(instanceId), eq("1H"), any(), any())).willReturn(List.of(cpuStat));
        given(instanceStatMemoryRepository.findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                eq(instanceId), eq("1H"), any(), any())).willReturn(List.of(memoryStat));
        given(instanceStatDiskRepository.findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                eq(instanceId), eq("1H"), any(), any())).willReturn(List.of(diskStat));
        given(instanceStatNetworkRepository.findAllByInstanceIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
                eq(instanceId), eq("1H"), any(), any())).willReturn(List.of(networkStat));

        // when
        InstanceHistoryMetricsResponse response = instanceService.getInstanceHistoryMetrics(instanceId, userId, yesterday);

        // then
        assertThat(response.instanceId()).isEqualTo(instanceId);
        assertThat(response.date()).isEqualTo(yesterday);
        assertThat(response.summary().cpu().cpuUsageAvg()).isEqualTo(45.0);
        assertThat(response.summary().memory().memAvailableMin()).isEqualTo(1_500_000_000L);
        assertThat(response.summary().disk().diskUsedPctMax()).isEqualTo(60.0);
        assertThat(response.summary().network().rxMbpsAvg()).isEqualTo(12.0);
        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).statTime()).isEqualTo(statTime);
    }

    @Test
    @DisplayName("오늘 날짜 요청 시 INVALID_HISTORICAL_DATE 예외 발생")
    void getInstanceHistoryMetrics_todayDate_throwsException() {
        // given
        Instance instance = ownedInstance();
        given(instanceRepository.findById(instanceId)).willReturn(Optional.of(instance));

        // when & then
        assertThatThrownBy(() -> instanceService.getInstanceHistoryMetrics(instanceId, userId, LocalDate.now()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", InstanceErrorCode.INVALID_HISTORICAL_DATE);
    }

    @Test
    @DisplayName("존재하지 않는 인스턴스의 과거 통계 조회 시 INSTANCE_NOT_FOUND 예외 발생")
    void getInstanceHistoryMetrics_instanceNotFound_throwsException() {
        // given
        given(instanceRepository.findById(instanceId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> instanceService.getInstanceHistoryMetrics(instanceId, userId, LocalDate.now().minusDays(1)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", InstanceErrorCode.INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 소유 인스턴스의 과거 통계 조회 시 INSTANCE_ACCESS_DENIED 예외 발생")
    void getInstanceHistoryMetrics_otherUser_throwsException() {
        // given
        Instance instance = ownedInstance();
        given(instanceRepository.findById(instanceId)).willReturn(Optional.of(instance));

        // when & then
        assertThatThrownBy(() -> instanceService.getInstanceHistoryMetrics(instanceId, 999L, LocalDate.now().minusDays(1)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", InstanceErrorCode.INSTANCE_ACCESS_DENIED);
    }
}
