package com.moni.api.domain.server.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
