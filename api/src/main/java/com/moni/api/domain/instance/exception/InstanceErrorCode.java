package com.moni.api.domain.instance.exception;

import com.moni.api.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InstanceErrorCode implements ErrorCode {

    INSTANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "존재하지 않는 인스턴스입니다."),
    THRESHOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "I002", "존재하지 않는 임계치입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    InstanceErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}