package com.moni.api.domain.instance.repository;

import com.moni.api.domain.instance.entity.Instance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceRepository extends JpaRepository<Instance, Long> {
}
