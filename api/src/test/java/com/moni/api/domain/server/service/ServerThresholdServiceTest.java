package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.server.dto.request.ServerThresholdItemRequest;
import com.moni.api.domain.server.dto.request.ServerThresholdsPatchRequest;
import com.moni.api.domain.server.dto.response.ServerThresholdResponse;
import com.moni.api.domain.server.dto.response.ServerThresholdUpdateResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServerThresholdServiceTest {

    @InjectMocks
    private ServerThresholdService serverThresholdService;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ServerThresholdRepository serverThresholdRepository;

    @Test
    @DisplayName("서버 임계치 목록 조회 성공")
    void getThresholds_success() {
        // given
        Long serverId = 1L;
        Instance mockInstance = Mockito.mock(Instance.class);
        Server server = Server.builder()
                .instance(mockInstance)
                .name("payment-api")
                .port(8081)
                .build();
        ReflectionTestUtils.setField(server, "id", serverId);

        ServerThreshold threshold1 = ServerThreshold.builder()
                .server(server)
                .metricKey(ServerMetricKey.JVM_HEAP_USAGE)
                .warningValue(80.0)
                .criticalValue(90.0)
                .build();

        ServerThreshold threshold2 = ServerThreshold.builder()
                .server(server)
                .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                .warningValue(300.0)
                .criticalValue(800.0)
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);
        given(serverThresholdRepository.findByServerId(serverId)).willReturn(List.of(threshold1, threshold2));

        // when
        List<ServerThresholdResponse> response = serverThresholdService.getThresholds(serverId);

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getMetricKey()).isEqualTo(ServerMetricKey.JVM_HEAP_USAGE);
        assertThat(response.get(0).getIsCustomized()).isFalse();
        assertThat(response.get(1).getMetricKey()).isEqualTo(ServerMetricKey.HTTP_AVG_LATENCY);
        assertThat(response.get(1).getIsCustomized()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 서버 임계치 목록 조회 시 예외 발생")
    void getThresholds_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;

        given(serverRepository.existsById(serverId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverThresholdService.getThresholds(serverId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ServerErrorCode.SERVER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("서버 임계치 수정 성공")
    void updateThresholds_success() {
        // given
        Long serverId = 1L;
        Instance mockInstance = Mockito.mock(Instance.class);
        Server server = Server.builder()
                .instance(mockInstance)
                .name("payment-api")
                .port(8081)
                .build();
        ReflectionTestUtils.setField(server, "id", serverId);

        ServerThreshold existingThreshold = ServerThreshold.builder()
                .server(server)
                .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                .warningValue(500.0)
                .criticalValue(1000.0)
                .build();
        ReflectionTestUtils.setField(existingThreshold, "id", 100L);

        ServerThresholdItemRequest itemRequest = ServerThresholdItemRequest.builder()
                .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                .warningValue(250.0)
                .criticalValue(700.0)
                .build();

        ServerThresholdsPatchRequest patchRequest = ServerThresholdsPatchRequest.builder()
                .thresholds(List.of(itemRequest))
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);
        given(serverThresholdRepository.findByServerIdAndMetricKey(serverId, ServerMetricKey.HTTP_AVG_LATENCY))
                .willReturn(Optional.of(existingThreshold));

        // when
        ServerThresholdUpdateResponse response = serverThresholdService.updateThresholds(serverId, patchRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getServerId()).isEqualTo(serverId);
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(existingThreshold.getWarningValue()).isEqualTo(250.0);
        assertThat(existingThreshold.getCriticalValue()).isEqualTo(700.0);
    }

    @Test
    @DisplayName("존재하지 않는 서버 ID로 요청 시 SERVER_NOT_FOUND 예외 발생")
    void updateThresholds_serverNotFound_throwsException() {
        // given
        Long serverId = 999L;
        ServerThresholdsPatchRequest patchRequest = ServerThresholdsPatchRequest.builder()
                .thresholds(List.of(ServerThresholdItemRequest.builder()
                        .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                        .warningValue(300.0)
                        .criticalValue(800.0)
                        .build()))
                .build();

        given(serverRepository.existsById(serverId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverThresholdService.updateThresholds(serverId, patchRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ServerErrorCode.SERVER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("경고 임계값이 심각 임계값 이상인 경우 INVALID_THRESHOLD_VALUE 예외 발생")
    void updateThresholds_invalidThresholdValue_throwsException() {
        // given
        Long serverId = 1L;

        ServerThresholdsPatchRequest patchRequest = ServerThresholdsPatchRequest.builder()
                .thresholds(List.of(ServerThresholdItemRequest.builder()
                        .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                        .warningValue(900.0)
                        .criticalValue(800.0)
                        .build()))
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> serverThresholdService.updateThresholds(serverId, patchRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ServerErrorCode.INVALID_THRESHOLD_VALUE.getMessage());
    }

    @Test
    @DisplayName("해당 지표의 임계치 설정이 없을 경우 SERVER_THRESHOLD_NOT_FOUND 예외 발생")
    void updateThresholds_thresholdNotFound_throwsServerThresholdNotFoundException() {
        // given
        Long serverId = 1L;
        ServerThresholdsPatchRequest patchRequest = ServerThresholdsPatchRequest.builder()
                .thresholds(List.of(ServerThresholdItemRequest.builder()
                        .metricKey(ServerMetricKey.HTTP_AVG_LATENCY)
                        .warningValue(300.0)
                        .criticalValue(800.0)
                        .build()))
                .build();

        given(serverRepository.existsById(serverId)).willReturn(true);
        given(serverThresholdRepository.findByServerIdAndMetricKey(serverId, ServerMetricKey.HTTP_AVG_LATENCY))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverThresholdService.updateThresholds(serverId, patchRequest))
                .isInstanceOf(CustomException.class)
                .hasMessage(ServerErrorCode.SERVER_THRESHOLD_NOT_FOUND.getMessage());
    }
}
