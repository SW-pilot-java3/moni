package com.moni.api.domain.server.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServerThresholdsPatchRequest {

    @NotNull(message = "리스트는 필수입니다.")
    @NotEmpty(message = "최소 하나 이상의 임계치 항목을 전송해야 합니다.")
    @Valid
    private List<ServerThresholdItemRequest> thresholds;
}
