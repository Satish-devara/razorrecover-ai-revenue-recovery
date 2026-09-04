package com.razorrecover.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorrecover.domain.AuditEvent;
import com.razorrecover.domain.enums.AuditEventType;
import com.razorrecover.repository.AuditEventRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(
            String correlationId,
            String aggregateType,
            UUID aggregateId,
            AuditEventType eventType,
            Map<String, ?> payload) {
        auditEventRepository.save(AuditEvent.create(
                correlationId,
                aggregateType,
                aggregateId,
                eventType.name(),
                "SYSTEM",
                serialize(payload)));
    }

    private String serialize(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit payload", exception);
        }
    }
}
