package com.razorrecover.repository;

import com.razorrecover.domain.RecoveryCase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, UUID> {
    Optional<RecoveryCase> findByCorrelationId(String correlationId);

    Optional<RecoveryCase> findByPaymentId(UUID paymentId);
}
