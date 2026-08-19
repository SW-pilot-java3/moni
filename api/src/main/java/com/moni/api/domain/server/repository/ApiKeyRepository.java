package com.moni.api.domain.server.repository;

import com.moni.api.domain.server.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByServerIdAndRevokedAtIsNull(Long serverId);

    Optional<ApiKey> findFirstByServerIdOrderByCreatedAtDesc(Long serverId);

    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<ApiKey> findByServerIdOrderByCreatedAtDesc(Long serverId);
}
