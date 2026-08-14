package com.moni.api.domain.server.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final InstanceService instanceService;

    @Transactional
    public ServerResponse createServer(ServerCreateRequest request) {
        Instance instance = instanceService.getInstanceById(request.getInstanceId());

        Server server = Server.builder()
                .instance(instance)
                .name(request.getName())
                .port(request.getPort())
                .build();

        Server savedServer = serverRepository.save(server);
        return ServerResponse.from(savedServer);
    }

    @Transactional
    public ServerResponse updateServer(Long serverId, ServerUpdateRequest request) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        server.updateServerInfo(request.getName(), request.getPort());
        return ServerResponse.from(server);
    }

    // --- [본인이 작성한 조회 로직] ---

    // 1. 특정 서버 상세 조회
    public ServerResponse getServerDetail(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));
        return ServerResponse.from(server);
    }

    // 2. 특정 인스턴스 내 서버 목록 조회
    public List<ServerResponse> getServerList(Long instanceId) {
        List<Server> servers = serverRepository.findByInstanceId(instanceId);
        return servers.stream()
                .map(ServerResponse::from)
                .toList();
    }
}