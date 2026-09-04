# Security and safety boundaries

## Secrets

Credentials are environment variables only. `.env` is ignored by Git and `.env.example` contains names only. Razorpay keys, LLM keys, database passwords, and tokens must never be committed or logged.

## Payment safety

The application will use Razorpay Test Mode only if an adapter is configured. The required simulation provider works without external credentials. No production payment behavior, refunds, or arbitrary amount changes are in scope.

## Bounded autonomy

An LLM is advisory. The backend will deterministically enforce retry ceilings, time windows, amount limits, cooldowns, idempotency, merchant authorization, explicit stop rules, and escalation requirements. Tool inputs will be validated and scope-limited.

## Auditability

Every decision will retain payment/case identifiers, failure context, retrieved policy references, recommended action, confidence, safety-check results, final action, outcome, timestamp, and correlation ID. Concise decision evidence is stored; hidden reasoning is not.

## Operational controls

Future endpoints will use DTO validation and meaningful errors. Logs will use correlation IDs and must redact secrets. Database migrations will be versioned through Flyway. Authentication is deliberately deferred until core workflow functionality is stable.
