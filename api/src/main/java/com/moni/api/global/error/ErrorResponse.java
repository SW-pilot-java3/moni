package com.moni.api.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> errors;

    private ErrorResponse(String code, String message, List<FieldErrorDetail> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), FieldErrorDetail.of(bindingResult));
    }

    @Getter
    public static class FieldErrorDetail {
        private final String field;
        private final String value;
        private final String reason;

        private FieldErrorDetail(String field, String value, String reason) {
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        public static List<FieldErrorDetail> of(BindingResult bindingResult) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            List<FieldErrorDetail> result = new ArrayList<>();
            for (FieldError error : fieldErrors) {
                Object rejectedValue = error.getRejectedValue();
                result.add(new FieldErrorDetail(
                        error.getField(),
                        rejectedValue == null ? "" : rejectedValue.toString(),
                        error.getDefaultMessage()
                ));
            }
            return result;
        }
    }
}