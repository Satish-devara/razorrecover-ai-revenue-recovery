package com.razorrecover.api;

import com.razorrecover.dto.AuditEventResponse;
import com.razorrecover.dto.CreateRecoveryCaseRequest;
import com.razorrecover.dto.RecoveryCaseResponse;
import com.razorrecover.dto.RecoveryDecisionResponse;
import com.razorrecover.recovery.RecoveryCaseService;
import com.razorrecover.recovery.RecoveryEngine;
import com.razorrecover.support.OperationContext;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recovery-cases")
public class RecoveryCaseController {

    private final RecoveryCaseService recoveryCaseService;
    private final RecoveryEngine recoveryEngine;

    public RecoveryCaseController(
            RecoveryCaseService recoveryCaseService,
            RecoveryEngine recoveryEngine) {

        this.recoveryCaseService = recoveryCaseService;
        this.recoveryEngine = recoveryEngine;
    }

    @PostMapping
    public ResponseEntity<RecoveryCaseResponse> open(
            @Valid @RequestBody CreateRecoveryCaseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recoveryCaseService.open(
                        request,
                        OperationContext.from(idempotencyKey)));
    }

    @PostMapping("/{recoveryCaseId}/evaluate")
    public RecoveryDecisionResponse evaluate(
            @PathVariable UUID recoveryCaseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return recoveryEngine.evaluate(
                recoveryCaseId,
                OperationContext.from(idempotencyKey));
    }

    @GetMapping
    public List<RecoveryCaseResponse> list() {
        return recoveryCaseService.list();
    }

    @GetMapping("/{recoveryCaseId}")
    public RecoveryCaseResponse get(@PathVariable UUID recoveryCaseId) {
        return recoveryCaseService.get(recoveryCaseId);
    }

    @GetMapping("/{recoveryCaseId}/decisions")
    public List<RecoveryDecisionResponse> decisions(
            @PathVariable UUID recoveryCaseId) {

        return recoveryCaseService.decisions(recoveryCaseId);
    }

    @GetMapping("/{recoveryCaseId}/audit-events")
    public List<AuditEventResponse> auditEvents(
            @PathVariable UUID recoveryCaseId) {

        return recoveryCaseService.auditEvents(recoveryCaseId);
    }
}
