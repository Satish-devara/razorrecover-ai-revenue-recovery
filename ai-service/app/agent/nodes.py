import os
import time

from dotenv import load_dotenv
from google import genai
from pydantic import BaseModel, Field

from app.agent.policy_retriever import retrieve_policy
from app.agent.state import RecoveryState

load_dotenv()


def classify_failure(state: RecoveryState) -> RecoveryState:
    failure_reason = state.get("failure_reason", "").upper()

    retryable_failures = {
        "TIMEOUT",
        "TEMPORARY_NETWORK_ERROR",
        "BANK_UNAVAILABLE",
    }

    customer_action_failures = {
        "PAYMENT_METHOD_ISSUE",
        "INSUFFICIENT_FUNDS",
        "ABANDONED",
    }

    if failure_reason in retryable_failures:
        category = "TRANSIENT"
    elif failure_reason in customer_action_failures:
        category = "CUSTOMER_ACTION_REQUIRED"
    else:
        category = "PERMANENT"

    return {
        **state,
        "failure_category": category,
    }


def retrieve_recovery_policy(state: RecoveryState) -> RecoveryState:
    """
    Retrieve the recovery policy through the MCP policy tool.

    MCP is the primary policy retrieval mechanism.
    The existing policy retriever is kept as a fallback.
    """

    from app.mcp.server import get_recovery_policy

    failure_reason = state.get("failure_reason", "")

    result = get_recovery_policy(failure_reason)

    if not result.get("success"):
        policy_id, policy_content = retrieve_policy(
            failure_reason
        )

        return {
            **state,
            "policy_id": policy_id,
            "retrieved_policy": policy_content,
        }

    return {
        **state,
        "policy_id": result["policy_id"],
        "retrieved_policy": result["policy"],
    }


class RecoveryDecision(BaseModel):
    recommended_action: str = Field(
        description=(
            "The recommended recovery action. "
            "Must be one of RETRY_PAYMENT, "
            "REQUEST_CUSTOMER_ACTION, or ESCALATE."
        )
    )

    confidence: float = Field(
        description="Confidence between 0 and 1."
    )

    reason: str = Field(
        description="Short business reason for the recommendation."
    )


def fallback_recovery_decision(
    state: RecoveryState,
) -> RecoveryState:
    """
    Deterministic safety fallback used when Gemini
    is unavailable.

    This guarantees that the recovery system can
    still produce a bounded recommendation.
    """

    failure_category = state.get(
        "failure_category",
        "PERMANENT",
    )

    if failure_category == "TRANSIENT":
        action = "RETRY_PAYMENT"

        reason = (
            "The payment failure is transient and "
            "the retrieved recovery policy permits "
            "an automatic retry."
        )

    elif failure_category == "CUSTOMER_ACTION_REQUIRED":
        action = "REQUEST_CUSTOMER_ACTION"

        reason = (
            "The payment failure requires customer "
            "intervention according to the retrieved "
            "recovery policy."
        )

    else:
        action = "ESCALATE"

        reason = (
            "The payment failure is not safely "
            "recoverable automatically and should "
            "be escalated."
        )

    return {
        **state,
        "recommended_action": action,
        "confidence": 0.85,
        "reason": reason,
    }


def decide_recovery_action(
    state: RecoveryState,
) -> RecoveryState:

    try:
        api_key = os.getenv("GEMINI_API_KEY")

        if not api_key:
            raise RuntimeError(
                "GEMINI_API_KEY is not configured"
            )

        client = genai.Client(
            api_key=api_key
        )

        model = os.getenv(
            "GEMINI_MODEL",
            "gemini-3.8-flash",
        )

        prompt = f"""
You are the AI recovery decision engine
for RazorRecover.

Your task is to recommend ONE safe recovery
action for a failed payment.

Payment information:

Amount: {state.get("amount")}
Currency: {state.get("currency")}
Failure reason: {state.get("failure_reason")}
Failure category: {state.get("failure_category")}
Scenario: {state.get("scenario")}

Retrieved recovery policy:

{state.get("retrieved_policy")}

Rules:

1. Follow the retrieved recovery policy.
2. Never invent a new recovery action.
3. Never modify the payment amount.
4. Never recommend unlimited retries.
5. Choose exactly ONE action:
   RETRY_PAYMENT
   REQUEST_CUSTOMER_ACTION
   ESCALATE
6. Return a concise business reason.
7. Confidence must be between 0 and 1.
8. Do not provide chain-of-thought.
9. The Java safety layer is authoritative
   and can reject your recommendation.

Return ONLY valid JSON:

{{
  "recommended_action": "RETRY_PAYMENT",
  "confidence": 0.95,
  "reason": "Short business reason"
}}
"""

        response = None

        # Retry Gemini up to 3 times.
        for attempt in range(3):
            try:
                response = client.models.generate_content(
                    model=model,
                    contents=prompt,
                    config={
                        "response_mime_type": "application/json",
                        "response_schema": RecoveryDecision,
                    },
                )

                print(
                    f"Gemini decision succeeded "
                    f"on attempt {attempt + 1}/3"
                )

                break

            except Exception as exception:
                print(
                    f"Gemini attempt "
                    f"{attempt + 1}/3 failed: "
                    f"{type(exception).__name__}"
                )

                if attempt < 2:
                    time.sleep(2)
                else:
                    raise exception

        if response is None:
            raise RuntimeError(
                "Gemini returned no response"
            )

        decision = RecoveryDecision.model_validate_json(
            response.text
        )

        return {
            **state,
            "recommended_action": decision.recommended_action,
            "confidence": decision.confidence,
            "reason": decision.reason,
        }

    except Exception as exception:

        print(
            "Gemini unavailable. "
            "Using deterministic policy fallback: "
            f"{type(exception).__name__}"
        )

        return fallback_recovery_decision(state)