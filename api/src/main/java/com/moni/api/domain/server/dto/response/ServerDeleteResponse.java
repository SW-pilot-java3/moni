package com.moni.api.domain.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServerDeleteResponse {

    private final Long deletedServerId;

    public static ServerDeleteResponse from(Long deletedServerId) {
        return new ServerDeleteResponse(deletedServerId);
    }
}
