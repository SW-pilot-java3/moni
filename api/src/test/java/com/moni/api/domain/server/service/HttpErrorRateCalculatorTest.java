package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpErrorRateCalculatorTest {

    @Test
    @DisplayName("5xx 요청 수 델타 / 전체 요청 수 델타로 에러율(%)을 계산한다")
    void calculate_returnsErrorRatePercent() {
        ServerHttpEndpointMetric prevOk = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(0L)
                .build();
        ServerHttpEndpointMetric currOk = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(90L) // countDelta=90
                .build();
        ServerHttpEndpointMetric prevError = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("500")
                .requestsCount(0L)
                .build();
        ServerHttpEndpointMetric currError = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("500")
                .requestsCount(10L) // countDelta=10
                .build();
        // 전체 100건 중 5xx 10건 -> 10%

        Double result = HttpErrorRateCalculator.calculate(
                List.of(prevOk, prevError), List.of(currOk, currError));

        assertThat(result).isCloseTo(10.0, within(0.001));
    }

    @Test
    @DisplayName("4xx는 에러로 집계하지 않는다")
    void calculate_excludes4xxFromErrorCount() {
        ServerHttpEndpointMetric prevOk = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(0L)
                .build();
        ServerHttpEndpointMetric currOk = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(90L)
                .build();
        ServerHttpEndpointMetric prevClientError = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("404")
                .requestsCount(0L)
                .build();
        ServerHttpEndpointMetric currClientError = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("404")
                .requestsCount(10L)
                .build();

        Double result = HttpErrorRateCalculator.calculate(
                List.of(prevOk, prevClientError), List.of(currOk, currClientError));

        assertThat(result).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("이전 레코드에 없던 신규 엔드포인트는 이번 윈도우 판정에서 스킵한다")
    void calculate_skipsNewlyAppearedEndpoint() {
        ServerHttpEndpointMetric currNew = ServerHttpEndpointMetric.builder()
                .uri("/api/new").method("GET").status("500")
                .requestsCount(10L)
                .build();

        assertThat(HttpErrorRateCalculator.calculate(List.of(), List.of(currNew))).isNull();
    }

    @Test
    @DisplayName("previous 또는 current 리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(HttpErrorRateCalculator.calculate(null, List.of())).isNull();
        assertThat(HttpErrorRateCalculator.calculate(List.of(), null)).isNull();
    }
}
