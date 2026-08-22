package com.moni.api.domain.server.validator;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerValidator {

    private final ServerRepository serverRepository;
    private final InstanceService instanceService;

    public Server validateAndGetServer(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new CustomException(ServerErrorCode.SERVER_NOT_FOUND));

        if (!server.isOwnedBy(userId)) {
            throw new CustomException(ServerErrorCode.SERVER_ACCESS_DENIED);
        }

        return server;
    }

    public Instance validateAndGetInstance(Long instanceId, Long userId) {
        return instanceService.getInstanceOwnedBy(instanceId, userId);
    }
}
