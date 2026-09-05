# Bank Unavailable Recovery Policy

Policy ID: POL-BANK-001

## Trigger

Use this policy when the customer's bank is temporarily unavailable.

## Recommended Action

RETRY_PAYMENT

## Rules

- Treat bank unavailability as a transient failure.
- Automatic retry is allowed when the recovery case is eligible.
- Maximum automatic retries: 3.
- Minimum cooldown between retries: 5 minutes.
- Recovery window: 24 hours.
- Do not change the payment amount.
- Stop when the retry limit is reached.
- Escalate if automatic recovery is no longer safe.

## Reason

Temporary bank availability problems may resolve without customer intervention.