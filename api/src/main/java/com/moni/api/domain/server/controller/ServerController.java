package com.moni.api.domain.server.controller;

import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.service.ServerService;
import com.moni.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @Valid @RequestBody ServerCreateRequest request) {
        ServerResponse response = serverService.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PatchMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> updateServer(
            @PathVariable Long serverId,
            @Valid @RequestBody ServerUpdateRequest request) {
        ServerResponse response = serverService.updateServer(serverId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerDeleteResponse>> deleteServer(
            @PathVariable Long serverId) {
        ServerDeleteResponse response = serverService.deleteServer(serverId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> getServerDetail(
            @PathVariable Long serverId) {
        ServerResponse response = serverService.getServerDetail(serverId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getServerList(
            @RequestParam Long instanceId) {
        List<ServerResponse> response = serverService.getServerList(instanceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}