# Evaluation methodology

## Goal

Evaluation will measure actual outcomes from a deterministic simulation, not manufactured dashboard figures. The planned engine will process batches of 100, 500, 1,000, and 10,000 synthetic payment failures.

## Scenarios

The simulator will provide seeded, repeatable cases for temporary network failures, timeouts, bank unavailability, repeated failure, permanently failed payments, customer payment-method issues, successful retries, and abandoned payments.

## Strategies

| Strategy | Definition |
| --- | --- |
| Baseline | Retry every eligible payment exactly once |
| RazorRecover | Policy-aware agent recommendation filtered by deterministic safety checks |

## Metrics

- payments analyzed
- revenue at risk
- eligible recovery attempts
- successful recoveries
- recovered revenue
- recovery rate
- retry success rate
- escalation rate
- stopped attempts
- average recovery time
- incorrect intervention rate, when simulator ground truth permits it

## Fairness and reproducibility

Both strategies will consume the same seeded input set and provider simulation rules. Run metadata, seed, strategy version, policy version, timestamps, and aggregate results will be saved in `ExperimentRun`. A result will be labeled as simulation data in the dashboard.

No results exist in Phase 1.
