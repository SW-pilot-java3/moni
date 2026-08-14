package com.moni.api.domain.server.exception;

import com.moni.api.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServerErrorCode implements ErrorCode {

    SERVER_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 서버입니다."),
    INVALID_THRESHOLD_VALUE(HttpStatus.BAD_REQUEST, "S002", "경고 임계값은 심각 임계값보다 작아야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ServerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
