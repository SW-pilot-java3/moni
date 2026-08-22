package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatJvm;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatJvmRepository extends JpaRepository<StatJvm, Long> {

    Optional<StatJvm> findFirstByServerIdOrderByStatTimeDesc(Long serverId);

    List<StatJvm> findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long serverId, String timeWindow, LocalDateTime from, LocalDateTime to);
}
