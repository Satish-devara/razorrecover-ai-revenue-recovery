# Payment Method Issue Recovery Policy

Policy ID: POL-PAYMENT-METHOD-001

## Trigger

Use this policy when the payment method has an issue.

## Recommended Action

REQUEST_CUSTOMER_ACTION

## Rules

- Do not automatically retry repeatedly.
- Ask the customer to use another payment method or correct the existing payment method.
- Do not modify the payment amount.
- Escalate the recovery case when customer action is required.

## Reason

Payment-method problems generally require customer intervention rather than repeated automatic retries.