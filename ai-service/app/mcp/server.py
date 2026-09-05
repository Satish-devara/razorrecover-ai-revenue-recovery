from pathlib import Path
from typing import Any

from mcp.server.mcpserver import MCPServer

mcp = MCPServer("RazorRecover MCP")

POLICY_DIRECTORY = (
    Path(__file__).resolve().parent.parent / "policies"
)

POLICY_MAPPING = {
    "TIMEOUT": "timeout.md",
    "TEMPORARY_NETWORK_ERROR": "timeout.md",
    "BANK_UNAVAILABLE": "bank_unavailable.md",
    "PAYMENT_METHOD_ISSUE": "payment_method_issue.md",
    "INSUFFICIENT_FUNDS": "payment_method_issue.md",
    "ABANDONED": "payment_method_issue.md",
    "PERMANENT_FAILURE": "permanent_failure.md",
}


@mcp.tool()
def get_recovery_policy(
    failure_reason: str,
) -> dict[str, Any]:
    """
    Retrieve the policy associated with a payment failure reason.
    This is a read-only tool.
    """

    normalized_reason = failure_reason.upper()

    filename = POLICY_MAPPING.get(
        normalized_reason,
        "permanent_failure.md",
    )

    policy_path = POLICY_DIRECTORY / filename

    if not policy_path.exists():
        return {
            "success": False,
            "failure_reason": normalized_reason,
            "error": f"Recovery policy not found: {filename}",
        }

    content = policy_path.read_text(
        encoding="utf-8"
    )

    policy_id = "UNKNOWN"

    for line in content.splitlines():
        if line.startswith("Policy ID"):
            policy_id = line.split(
                ":", 1
            )[1].strip()
            break

    return {
        "success": True,
        "failure_reason": normalized_reason,
        "policy_id": policy_id,
        "policy_file": filename,
        "policy": content,
    }


@mcp.tool()
def get_payment_context(
    payment_id: str,
    amount: float,
    currency: str,
    failure_reason: str,
    scenario: str,
) -> dict[str, Any]:
    """
    Retrieve normalized payment context for AI analysis.
    This is read-only and cannot mutate payment state.
    """

    return {
        "success": True,
        "payment_id": payment_id,
        "amount": amount,
        "currency": currency,
        "failure_reason": failure_reason,
        "scenario": scenario,
        "mutation_allowed": False,
        "message": (
            "Payment context is available for analysis. "
            "Payment mutations must be executed by the "
            "Java safety layer."
        ),
    }


if __name__ == "__main__":
    mcp.run()
