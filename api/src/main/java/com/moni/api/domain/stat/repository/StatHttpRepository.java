package com.moni.api.domain.stat.repository;

import com.moni.api.domain.stat.entity.StatHttp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatHttpRepository extends JpaRepository<StatHttp, Long> {

    Optional<StatHttp> findFirstByServerIdOrderByStatTimeDesc(Long serverId);

    List<StatHttp> findAllByServerIdAndTimeWindowAndStatTimeBetweenOrderByStatTimeAsc(
            Long serverId, String timeWindow, LocalDateTime from, LocalDateTime to);
}
