from typing import TypedDict


class RecoveryState(TypedDict, total=False):
    recovery_case_id: str
    payment_id: str

    amount: float
    currency: str

    failure_reason: str
    scenario: str

    failure_category: str

    retrieved_policy: str
    policy_id: str

    recommended_action: str
    confidence: float
    reason: str

    safety_status: str
    final_action: str
    outcome: str