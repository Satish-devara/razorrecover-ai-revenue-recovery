# Phase 3 API reference

All endpoints are local simulator endpoints under `/api`. JSON errors use the `ApiError` envelope. An optional `Idempotency-Key` request header is accepted at mutating service boundaries; durable idempotency enforcement is intentionally deferred to Phase 4.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/payments` | Create a simulated payment; it processes immediately unless `processImmediately` is `false`. |
| `GET` | `/api/payments` | List payments. |
| `GET` | `/api/payments/{paymentId}` | Retrieve a payment. |
| `POST` | `/api/payments/{paymentId}/simulate-failure` | Process the initial attempt of a pending payment using a failure scenario. |
| `POST` | `/api/payments/{paymentId}/retries` | Execute the next deterministic retry for a failed payment. |
| `GET` | `/api/payments/{paymentId}/attempts` | List payment attempts. |
| `POST` | `/api/recovery-cases` | Explicitly open a recovery case for a failed payment. |
| `GET` | `/api/recovery-cases` | List recovery cases. |
| `GET` | `/api/recovery-cases/{recoveryCaseId}` | Retrieve a recovery case. |
| `GET` | `/api/recovery-cases/{recoveryCaseId}/decisions` | Retrieve decision history (empty until a later phase). |
| `GET` | `/api/recovery-cases/{recoveryCaseId}/audit-events` | Retrieve correlated payment and recovery audit events. |

## Example

```bash
curl -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"amount":250.00,"currency":"INR","scenario":"RETRY_THEN_SUCCESS"}'

curl -X POST http://localhost:8080/api/payments/{paymentId}/retries
```

The initial attempt fails deterministically, the retry succeeds, and the system writes payment attempts plus `PAYMENT_CREATED`, `PAYMENT_FAILED`, `PAYMENT_RETRY_ATTEMPTED`, and `PAYMENT_RECOVERED` audit records.
