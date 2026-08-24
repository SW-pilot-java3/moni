package com.moni.api.domain.server.controller;

import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerThresholdsPatchRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerHistoryMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerRealtimeMetricsResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.dto.response.ServerSummaryResponse;
import com.moni.api.domain.server.dto.response.ServerThresholdResponse;
import com.moni.api.domain.server.dto.response.ServerThresholdUpdateResponse;
import com.moni.api.domain.server.service.ServerMetricService;
import com.moni.api.domain.server.service.ServerService;
import com.moni.api.domain.server.service.ServerSseService;
import com.moni.api.domain.server.service.ServerThresholdService;
import com.moni.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "서버")
@Validated
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final ServerThresholdService serverThresholdService;
    private final ServerMetricService serverMetricService;
    private final ServerSseService serverSseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ServerCreateRequest request) {
        ServerResponse response = serverService.createServer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> updateServer(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ServerUpdateRequest request) {
        ServerResponse response = serverService.updateServer(serverId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerDeleteResponse>> deleteServer(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        ServerDeleteResponse response = serverService.deleteServer(serverId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serverId}/thresholds")
    public ResponseEntity<ApiResponse<List<ServerThresholdResponse>>> getThresholds(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        List<ServerThresholdResponse> response = serverThresholdService.getThresholds(serverId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{serverId}/thresholds")
    public ResponseEntity<ApiResponse<ServerThresholdUpdateResponse>> updateThresholds(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ServerThresholdsPatchRequest request) {
        ServerThresholdUpdateResponse response = serverThresholdService.updateThresholds(serverId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> getServerDetail(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        ServerResponse response = serverService.getServerDetail(serverId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getServerList(
            @RequestParam Long instanceId,
            @AuthenticationPrincipal Long userId) {
        List<ServerResponse> response = serverService.getServerList(instanceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serverId}/metrics/realtime")
    public ResponseEntity<ApiResponse<ServerRealtimeMetricsResponse>> getServerRealtimeMetrics(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit) {
        ServerRealtimeMetricsResponse response = serverMetricService.getServerRealtimeMetrics(serverId, userId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serverId}/metrics/history")
    public ResponseEntity<ApiResponse<ServerHistoryMetricsResponse>> getServerHistoryMetrics(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) LocalDate date) {
        ServerHistoryMetricsResponse response = serverMetricService.getServerHistoryMetrics(serverId, userId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/metrics/summary")
    public ResponseEntity<ApiResponse<List<ServerSummaryResponse>>> getServerSummaryList(
            @RequestParam Long instanceId,
            @AuthenticationPrincipal Long userId) {
        List<ServerSummaryResponse> response = serverService.getServerSummaryList(instanceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/{serverId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamServerMetrics(
            @PathVariable Long serverId,
            @AuthenticationPrincipal Long userId) {
        return serverSseService.subscribe(serverId, userId);
    }
}