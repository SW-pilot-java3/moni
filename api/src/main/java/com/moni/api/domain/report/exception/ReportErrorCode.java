package com.moni.api.domain.report.exception;

import com.moni.api.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReportErrorCode implements ErrorCode {

    CLAUDE_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "R001", "AI 리포트 생성 요청에 실패했습니다."),
    CLAUDE_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "R002", "AI 응답을 해석하지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ReportErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
