package com.razorrecover.repository;

import com.razorrecover.domain.RecoveryPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryPolicyRepository extends JpaRepository<RecoveryPolicy, UUID> {
}
