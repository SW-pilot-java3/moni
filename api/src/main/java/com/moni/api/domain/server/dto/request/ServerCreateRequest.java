package com.moni.api.domain.server.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServerCreateRequest {

    @NotNull(message = "인스턴스 ID는 필수 입력 값입니다.")
    private Long instanceId;

    @NotBlank(message = "서버 명칭은 필수 입력 값입니다.")
    @Size(max = 100, message = "서버 명칭은 100자 이하이어야 합니다.")
    private String name;

    @NotNull(message = "포트 번호는 필수 입력 값입니다.")
    @Min(value = 1, message = "포트 번호는 1 이상이어야 합니다.")
    @Max(value = 65535, message = "포트 번호는 65535 이하이어야 합니다.")
    private Integer port;
}
