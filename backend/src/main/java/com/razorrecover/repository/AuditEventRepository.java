package com.razorrecover.repository;

import com.razorrecover.domain.AuditEvent;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);
}
