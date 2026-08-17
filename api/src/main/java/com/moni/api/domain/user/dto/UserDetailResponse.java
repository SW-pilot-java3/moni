package com.moni.api.domain.user.dto;

import com.moni.api.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long userId,
        String email,
        LocalDateTime createdAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
