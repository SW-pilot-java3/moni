package com.moni.api.domain.server.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.dto.response.ServerSummaryResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.stat.entity.StatHikariCp;
import com.moni.api.domain.stat.entity.StatHttp;
import com.moni.api.domain.stat.entity.StatJvm;
import com.moni.api.domain.stat.entity.StatThreadPool;
import com.moni.api.domain.stat.repository.StatHikariCpRepository;
import com.moni.api.domain.stat.repository.StatHttpRepository;
import com.moni.api.domain.stat.repository.StatJvmRepository;
import com.moni.api.domain.stat.repository.StatThreadPoolRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final InstanceService instanceService;

    private final StatJvmRepository statJvmRepository;
    private final StatHttpRepository statHttpRepository;
    private final StatHikariCpRepository statHikariCpRepository;
    private final StatThreadPoolRepository statThreadPoolRepository;

    public List<ServerSummaryResponse> getServerSummaryList(Long instanceId) {
        instanceService.getInstanceById(instanceId);

        List<Server> servers = serverRepository.findByInstanceId(instanceId);
        return servers.stream()
                .map(server -> {
                    StatJvm jvm = statJvmRepository.findFirstByServerIdOrderByStatTimeDesc(server.getId()).orElse(null);
                    StatHttp http = statHttpRepository.findFirstByServerIdOrderByStatTimeDesc(server.getId()).orElse(null);
                    StatHikariCp hikari = statHikariCpRepository.findFirstByServerIdOrderByStatTimeDesc(server.getId()).orElse(null);
                    StatThreadPool pool = statThreadPoolRepository.findFirstByServerIdOrderByStatTimeDesc(server.getId()).orElse(null);

                    return ServerSummaryResponse.from(server, jvm, http, hikari, pool);
                })
                .toList();
    }

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

    @Transactional
    public ServerDeleteResponse deleteServer(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        serverRepository.delete(server);
        return ServerDeleteResponse.from(serverId);
    }
}
