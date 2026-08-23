package com.moni.api.domain.instance.controller;

import com.moni.api.domain.instance.dto.*;
import com.moni.api.domain.instance.service.InstanceRealtimeMetricService;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.instance.sse.InstanceMetricSseEmitterRegistry;
import com.moni.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final InstanceRealtimeMetricService instanceRealtimeMetricService;
    private final InstanceMetricSseEmitterRegistry sseEmitterRegistry;

    @PostMapping
    public ResponseEntity<ApiResponse<InstanceCreateResponse>> createInstance(@AuthenticationPrincipal Long userId,
                                                                                @RequestBody @Valid InstanceCreateRequest request) {
        InstanceCreateResponse response = instanceService.createInstance(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InstanceListItemResponse>>> getInstances(@AuthenticationPrincipal Long userId) {
        List<InstanceListItemResponse> response = instanceService.getInstances(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @PatchMapping("/{instanceId}")
    public ResponseEntity<ApiResponse<InstanceUpdateResponse>> updateInstance(@PathVariable Long instanceId,
                                                                                @AuthenticationPrincipal Long userId,
                                                                                @RequestBody @Valid InstanceUpdateRequest request) {
        InstanceUpdateResponse response = instanceService.updateInstance(instanceId, userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{instanceId}")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable Long instanceId,
                                                              @AuthenticationPrincipal Long userId) {
        instanceService.deleteInstance(instanceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{instanceId}/thresholds")
    public ResponseEntity<ApiResponse<InstanceThresholdUpdateResponse>> updateThresholds(@PathVariable Long instanceId,
                                                                                         @AuthenticationPrincipal Long userId,
                                                                                         @RequestBody @Valid InstanceThresholdUpdateRequest request) {
        InstanceThresholdUpdateResponse response = instanceService.updateThresholds(instanceId, userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @GetMapping("/{instanceId}/thresholds")
    public ResponseEntity<ApiResponse<List<InstanceThresholdResponse>>> getThresholds(@PathVariable Long instanceId,
                                                                                        @AuthenticationPrincipal Long userId) {
        List<InstanceThresholdResponse> response = instanceService.getThresholds(instanceId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @GetMapping("/{instanceId}/metrics/realtime")
    public ResponseEntity<ApiResponse<List<InstanceRealtimeMetricResponse>>> getRecentRealtimeMetrics(@PathVariable Long instanceId,
                                                                                                         @AuthenticationPrincipal Long userId) {
        List<InstanceRealtimeMetricResponse> response = instanceService.getRecentRealtimeMetrics(instanceId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @GetMapping("/{instanceId}/metrics/history")
    public ResponseEntity<ApiResponse<InstanceHistoryMetricsResponse>> getInstanceHistoryMetrics(
            @PathVariable Long instanceId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) LocalDate date) {
        InstanceHistoryMetricsResponse response = instanceService.getInstanceHistoryMetrics(instanceId, userId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/{instanceId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(@PathVariable Long instanceId,
                                     @AuthenticationPrincipal Long userId) {
        instanceService.getInstanceOwnedBy(instanceId, userId);
        return sseEmitterRegistry.register(instanceId);
    }
}
