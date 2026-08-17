package com.moni.api.domain.auth.service;

import com.moni.api.domain.auth.dto.LoginRequest;
import com.moni.api.domain.auth.dto.LoginResponse;
import com.moni.api.domain.auth.dto.SignupRequest;
import com.moni.api.domain.auth.exception.AuthErrorCode;
import com.moni.api.domain.user.dto.UserDetailResponse;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.exception.UserErrorCode;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.exception.CustomException;
import com.moni.api.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDetailResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        try {
            User user = new User(
                    request.email(),
                    passwordEncoder.encode(request.password())
            );
            UserDetailResponse response = UserDetailResponse.from(userRepository.save(user));
            return response;
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(AuthErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId());

        return LoginResponse.of(accessToken, jwtProvider.getAccessTokenExpiration(), user);
    }
}