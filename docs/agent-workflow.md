# Agent workflow

## Scope

This is the target workflow for Phase 7, not a currently implemented agent. It is designed around bounded autonomy: the agent recommends; deterministic application code authorizes or rejects actions.

```mermaid
stateDiagram-v2
  [*] --> RECEIVE_PAYMENT_EVENT
  RECEIVE_PAYMENT_EVENT --> LOAD_PAYMENT_CONTEXT
  LOAD_PAYMENT_CONTEXT --> ANALYZE_FAILURE
  ANALYZE_FAILURE --> RETRIEVE_POLICY
  RETRIEVE_POLICY --> CALCULATE_RECOVERY_CONFIDENCE
  CALCULATE_RECOVERY_CONFIDENCE --> SELECT_RECOVERY_ACTION
  SELECT_RECOVERY_ACTION --> CHECK_SAFETY_AND_STOPPING_RULES
  CHECK_SAFETY_AND_STOPPING_RULES --> EXECUTE_ACTION: allowed
  CHECK_SAFETY_AND_STOPPING_RULES --> ESCALATE: blocked or human review
  EXECUTE_ACTION --> EVALUATE_RESULT
  EVALUATE_RESULT --> RECOVERED: payment succeeds
  EVALUATE_RESULT --> RETRY: eligible retry remains
  EVALUATE_RESULT --> ESCALATE: retry limit or review required
  EVALUATE_RESULT --> STOP: permanent failure or expired window
  RECOVERED --> WRITE_AUDIT_EVENT
  RETRY --> WRITE_AUDIT_EVENT
  ESCALATE --> WRITE_AUDIT_EVENT
  STOP --> WRITE_AUDIT_EVENT
  WRITE_AUDIT_EVENT --> [*]
```

## Planned input and output

The agent input includes a payment failure, relevant customer/payment history, prior recovery attempts, merchant configuration, and retrieved policies. Its output will contain an allowed action candidate, confidence, policy identifiers, and concise evidence. It will never persist a hidden chain-of-thought.

## Deterministic gate

Before a recommendation is executed, Spring Boot will verify all of the following:

- retry count is below a configured maximum;
- payment is inside the recovery window;
- amount is below the automatic-recovery ceiling;
- cooldown is satisfied;
- idempotency key is new;
- action is permitted by merchant and recovery policy;
- action does not alter amount, issue a refund, or exceed provider capability.

Any failed check produces an auditable stop or escalation outcome. The transition graph has no unbounded loop.
