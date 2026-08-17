package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatThreadPool;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatThreadPoolRepository extends JpaRepository<StatThreadPool, Long> {

    Optional<StatThreadPool> findFirstByServerIdOrderByStatTimeDesc(Long serverId);
}
