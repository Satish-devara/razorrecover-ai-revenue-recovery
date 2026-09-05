# Payment Timeout Recovery Policy

Policy ID: POL-TIMEOUT-001

## Trigger

Use this policy when a payment fails because of a timeout.

## Recommended Action

RETRY_PAYMENT

## Rules

- A timeout is considered a transient payment failure.
- Retry the payment automatically when the recovery case is eligible.
- Maximum automatic retries: 3.
- Minimum cooldown between retries: 5 minutes.
- Recovery window: 24 hours.
- Never modify the original payment amount.
- Never retry a payment that is already successful.
- Never retry a terminal recovery case.
- Stop automatic recovery when the retry limit is reached.
- Escalate when automatic recovery is no longer safe.

## Reason

A timeout may be caused by a temporary network or payment-processing problem. Retrying can recover revenue without requiring immediate customer intervention.