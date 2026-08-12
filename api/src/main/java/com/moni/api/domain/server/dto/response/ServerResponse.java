package com.moni.api.domain.server.dto.response;

import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerResponse {

    private Long serverId;
    private Long instanceId;
    private String name;
    private Integer port;
    private ServerStatus status;
    private LocalDateTime lastReceivedAt;
    private LocalDateTime createdAt;

    public static ServerResponse from(Server server) {
        return ServerResponse.builder()
                .serverId(server.getId())
                .instanceId(server.getInstance().getId())
                .name(server.getName())
                .port(server.getPort())
                .status(server.getStatus())
                .lastReceivedAt(server.getLastReceivedAt())
                .createdAt(server.getCreatedAt())
                .build();
    }
}
