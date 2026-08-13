package com.moni.api.domain.instance.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.IpAddress;

public record InstanceCreateRequest(
        @NotBlank String name,
        @NotBlank @IpAddress(type = IpAddress.Type.IPv4) String ip
) {
}