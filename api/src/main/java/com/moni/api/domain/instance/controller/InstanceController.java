package com.moni.api.domain.instance.controller;

import com.moni.api.domain.instance.dto.*;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InstanceCreateResponse>> createInstance(Long userId, @RequestBody @Valid InstanceCreateRequest request) {
        InstanceCreateResponse response = instanceService.createInstance(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{instanceId}")
    public ResponseEntity<ApiResponse<InstanceUpdateResponse>> updateInstance(@PathVariable Long instanceId, @RequestBody @Valid InstanceUpdateRequest request) {
        InstanceUpdateResponse response = instanceService.updateInstance(instanceId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{instanceId}")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable Long instanceId) {
        instanceService.deleteInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{instanceId}/thresholds")
    public ResponseEntity<ApiResponse<InstanceThresholdUpdateResponse>> updateThresholds(@PathVariable Long instanceId,
                                                                                         @RequestBody @Valid InstanceThresholdUpdateRequest request) {
        InstanceThresholdUpdateResponse response = instanceService.updateThresholds(instanceId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }
}
