package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatHttp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatHttpRepository extends JpaRepository<StatHttp, Long> {

    Optional<StatHttp> findFirstByServerIdOrderByStatTimeDesc(Long serverId);
}
