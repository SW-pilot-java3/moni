package com.moni.api.domain.metric.controller;

import com.moni.api.domain.metric.dto.request.MetricRecordRequest;
import com.moni.api.domain.metric.dto.response.MetricRecordResponse;
import com.moni.api.domain.metric.service.MetricService;
import com.moni.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메트릭 수집")
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    public ResponseEntity<ApiResponse<MetricRecordResponse>> recordMetrics(
            @RequestHeader(value = "X-API-KEY", required = false) String rawApiKey,
            @RequestBody @Valid MetricRecordRequest request) {
        MetricRecordResponse response = metricService.recordMetrics(rawApiKey, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
