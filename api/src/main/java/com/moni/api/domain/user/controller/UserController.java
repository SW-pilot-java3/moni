package com.moni.api.domain.user.controller;

import com.moni.api.domain.user.dto.UserDetailResponse;
import com.moni.api.domain.user.service.UserService;
import com.moni.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(@PathVariable Long userId) {
        UserDetailResponse response = userService.getUserDetail(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }
}