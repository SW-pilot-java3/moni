package com.moni.api.domain.auth.dto;

import com.moni.api.domain.user.entity.User;

public record LoginResponse(
    String tokenType,
    String accessToken,
    Long expiresIn,
    UserSummary user
) {
    public static LoginResponse of(String accessToken, long accessTokenExpirationMillis, User user) {
        return new LoginResponse(
                "Bearer",
                accessToken,
                accessTokenExpirationMillis / 1000,
                new UserSummary(user.getId(), user.getEmail())
        );
    }

    public record UserSummary(
            Long userId,
            String email
    ) {
    }
}
