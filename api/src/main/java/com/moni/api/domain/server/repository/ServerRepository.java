package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.Server;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findByInstanceId(Long instanceId);
}
