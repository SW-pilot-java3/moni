package com.moni.api.domain.auth.dto;

import com.moni.api.domain.user.dto.UserDetailResponse;
import com.moni.api.domain.user.entity.User;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String email,
        LocalDateTime createdAt
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
