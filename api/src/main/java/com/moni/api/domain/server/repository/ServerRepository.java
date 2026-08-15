package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {


    List<Server> findByInstanceId(Long instanceId);
}