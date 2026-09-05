package com.razorrecover.recovery;

import com.razorrecover.dto.AiRecoveryDecisionRequest;
import com.razorrecover.dto.RecoveryDecisionResponse;
import com.razorrecover.support.OperationContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recovery-cases")
public class RecoveryAiController {

    private final RecoveryEngine recoveryEngine;

    public RecoveryAiController(RecoveryEngine recoveryEngine) {
        this.recoveryEngine = recoveryEngine;
    }

    @PostMapping("/{recoveryCaseId}/ai-decision")
    public ResponseEntity<RecoveryDecisionResponse> evaluateAiDecision(
            @PathVariable UUID recoveryCaseId,
            @Valid @RequestBody AiRecoveryDecisionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey) {

        OperationContext context = OperationContext.from(idempotencyKey);

        RecoveryDecisionResponse response =
                recoveryEngine.evaluateAiDecision(
                        recoveryCaseId,
                        request.recommendedAction(),
                        request.confidence(),
                        request.reason(),
                        request.policyId(),
                        context
                );

        return ResponseEntity.ok(response);
    }
}
