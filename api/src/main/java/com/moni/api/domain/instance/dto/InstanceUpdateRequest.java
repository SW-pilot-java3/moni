package com.moni.api.domain.instance.dto;

import jakarta.validation.constraints.NotBlank;

public record InstanceUpdateRequest(
        @NotBlank String name,
        @NotBlank String ip
) {
}