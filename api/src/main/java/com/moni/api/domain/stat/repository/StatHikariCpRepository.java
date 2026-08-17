package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatHikariCp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatHikariCpRepository extends JpaRepository<StatHikariCp, Long> {

    Optional<StatHikariCp> findFirstByServerIdOrderByStatTimeDesc(Long serverId);
}
