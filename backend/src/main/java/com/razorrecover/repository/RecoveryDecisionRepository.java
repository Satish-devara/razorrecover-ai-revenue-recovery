package com.razorrecover.repository;

import com.razorrecover.domain.RecoveryDecision;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryDecisionRepository extends JpaRepository<RecoveryDecision, UUID> {
    List<RecoveryDecision> findByRecoveryCaseIdOrderByCreatedAtAsc(UUID recoveryCaseId);
}
