package com.moni.api.domain.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.moni.api.domain.server.entity.ServerHttpEndpointMetric;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpAvgLatencyCalculatorTest {

    @Test
    @DisplayName("(uri, method, status)로 매칭해 전체 엔드포인트 합산 Σsum/Σcount로 평균 응답지연시간(ms)을 계산한다")
    void calculate_returnsWeightedAverageAcrossEndpoints() {
        ServerHttpEndpointMetric prevA = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(100L).requestsSum(10.0) // 초 단위
                .build();
        ServerHttpEndpointMetric currA = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(200L).requestsSum(30.0) // countDelta=100, sumDelta=20.0
                .build();
        ServerHttpEndpointMetric prevB = ServerHttpEndpointMetric.builder()
                .uri("/api/b").method("GET").status("200")
                .requestsCount(0L).requestsSum(0.0)
                .build();
        ServerHttpEndpointMetric currB = ServerHttpEndpointMetric.builder()
                .uri("/api/b").method("GET").status("200")
                .requestsCount(100L).requestsSum(30.0) // countDelta=100, sumDelta=30.0
                .build();
        // 합산: sumDelta=50.0(초), countDelta=200 -> 평균 0.25초 = 250ms

        Double result = HttpAvgLatencyCalculator.calculate(
                List.of(prevA, prevB), List.of(currA, currB));

        assertThat(result).isCloseTo(250.0, within(0.001));
    }

    @Test
    @DisplayName("이전 레코드에 없던 신규 엔드포인트는 이번 윈도우 판정에서 스킵한다")
    void calculate_skipsNewlyAppearedEndpoint() {
        ServerHttpEndpointMetric currNew = ServerHttpEndpointMetric.builder()
                .uri("/api/new").method("GET").status("200")
                .requestsCount(10L).requestsSum(1.0)
                .build();

        assertThat(HttpAvgLatencyCalculator.calculate(List.of(), List.of(currNew))).isNull();
    }

    @Test
    @DisplayName("countDelta가 0 이하이면(카운터 리셋) 해당 엔드포인트는 제외한다")
    void calculate_nonPositiveCountDelta_excludesEndpoint() {
        ServerHttpEndpointMetric prev = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(100L).requestsSum(10.0)
                .build();
        ServerHttpEndpointMetric curr = ServerHttpEndpointMetric.builder()
                .uri("/api/a").method("GET").status("200")
                .requestsCount(50L).requestsSum(5.0) // 리셋되어 이전보다 작아짐
                .build();

        assertThat(HttpAvgLatencyCalculator.calculate(List.of(prev), List.of(curr))).isNull();
    }

    @Test
    @DisplayName("previous 또는 current 리스트가 null이면 null을 반환한다")
    void calculate_nullList_returnsNull() {
        assertThat(HttpAvgLatencyCalculator.calculate(null, List.of())).isNull();
        assertThat(HttpAvgLatencyCalculator.calculate(List.of(), null)).isNull();
    }
}
