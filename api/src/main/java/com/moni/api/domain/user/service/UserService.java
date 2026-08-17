package com.moni.api.domain.user.service;

import com.moni.api.domain.user.dto.UserDetailResponse;
import com.moni.api.domain.user.entity.User;
import com.moni.api.domain.user.exception.UserErrorCode;
import com.moni.api.domain.user.repository.UserRepository;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }
}