package com.razorrecover.repository;

import com.razorrecover.domain.RecoveryDecision;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryDecisionRepository extends JpaRepository<RecoveryDecision, UUID> {
}
