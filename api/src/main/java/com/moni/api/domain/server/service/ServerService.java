package com.moni.api.domain.server.service;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.dto.request.ServerCreateRequest;
import com.moni.api.domain.server.dto.request.ServerUpdateRequest;
import com.moni.api.domain.server.dto.response.ServerDeleteResponse;
import com.moni.api.domain.server.dto.response.ServerResponse;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.entity.ServerMetricKey;
import com.moni.api.domain.server.entity.ServerThreshold;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.server.repository.ServerThresholdRepository;
import com.moni.api.global.error.exception.CustomException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerThresholdRepository serverThresholdRepository;
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
        initDefaultThresholds(savedServer);

        return ServerResponse.from(savedServer);
    }

    private void initDefaultThresholds(Server server) {
        List<ServerThreshold> defaultThresholds = Arrays.stream(ServerMetricKey.values())
                .map(metricKey -> ServerThreshold.builder()
                        .server(server)
                        .metricKey(metricKey)
                        .warningValue(metricKey.getDefaultWarningValue())
                        .criticalValue(metricKey.getDefaultCriticalValue())
                        .build())
                .toList();

        serverThresholdRepository.saveAll(defaultThresholds);
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


    public ServerResponse getServerDetail(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));
        return ServerResponse.from(server);
    }


    public List<ServerResponse> getServerList(Long instanceId) {
        instanceService.getInstanceById(instanceId);
        List<Server> servers = serverRepository.findByInstanceId(instanceId);
        return servers.stream()
                .map(ServerResponse::from)
                .toList();
    }
}