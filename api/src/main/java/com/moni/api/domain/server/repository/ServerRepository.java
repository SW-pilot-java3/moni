package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {

    // 💡 이 한 줄을 추가해 주세요!
    List<Server> findByInstanceId(Long instanceId);
}