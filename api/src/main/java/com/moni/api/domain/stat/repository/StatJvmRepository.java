package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatJvm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatJvmRepository extends JpaRepository<StatJvm, Long> {

    Optional<StatJvm> findFirstByServerIdOrderByStatTimeDesc(Long serverId);
}
