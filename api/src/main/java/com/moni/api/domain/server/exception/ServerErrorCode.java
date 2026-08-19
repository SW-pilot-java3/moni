package com.moni.api.domain.server.exception;

import com.moni.api.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerErrorCode implements ErrorCode {

    SERVER_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 서버입니다."),
    INVALID_THRESHOLD_VALUE(HttpStatus.BAD_REQUEST, "S002", "경고 임계값은 심각 임계값보다 작아야 합니다."),
    SERVER_THRESHOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "S003", "존재하지 않는 서버 임계치 설정입니다."),
    API_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "S004", "이미 발급된 활성 API Key가 존재합니다. 재발급을 이용해주세요."),
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "S005", "활성화된 API Key가 존재하지 않습니다."),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "S006", "유효하지 않거나 만료된 API Key입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ServerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
