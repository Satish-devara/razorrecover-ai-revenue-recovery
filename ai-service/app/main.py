import os

import httpx
from fastapi import FastAPI
from pydantic import BaseModel

from app.agent.graph import recovery_graph


app = FastAPI(
    title="RazorRecover AI Service",
    version="1.0.0",
)


BACKEND_URL = os.getenv(
    "BACKEND_URL",
    "http://localhost:8080",
)


class RecoveryRequest(BaseModel):
    recovery_case_id: str
    payment_id: str
    amount: float
    currency: str
    failure_reason: str
    scenario: str


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "razorrecover-ai",
    }


@app.post("/agent/evaluate")
def evaluate_recovery(request: RecoveryRequest):

    initial_state = {
        "recovery_case_id": request.recovery_case_id,
        "payment_id": request.payment_id,
        "amount": request.amount,
        "currency": request.currency,
        "failure_reason": request.failure_reason,
        "scenario": request.scenario,
    }

    result = recovery_graph.invoke(initial_state)

    return result


@app.post("/agent/recover")
def recover_payment(request: RecoveryRequest):

    initial_state = {
        "recovery_case_id": request.recovery_case_id,
        "payment_id": request.payment_id,
        "amount": request.amount,
        "currency": request.currency,
        "failure_reason": request.failure_reason,
        "scenario": request.scenario,
    }

    result = recovery_graph.invoke(initial_state)

    ai_decision = {
        "recommendedAction": result["recommended_action"],
        "confidence": result["confidence"],
        "reason": result["reason"],
        "policyId": result["policy_id"],
    }

    response = httpx.post(
        f"{BACKEND_URL}/api/recovery-cases/"
        f"{request.recovery_case_id}/ai-decision",
        json=ai_decision,
        timeout=30.0,
    )

    response.raise_for_status()

    return {
        "ai_decision": ai_decision,
        "recovery_result": response.json(),
    }