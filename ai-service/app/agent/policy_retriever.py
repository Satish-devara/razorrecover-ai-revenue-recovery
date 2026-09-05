from pathlib import Path


POLICY_DIRECTORY = Path(__file__).resolve().parent.parent / "policies"


POLICY_MAPPING = {
    "TIMEOUT": "timeout.md",
    "TEMPORARY_NETWORK_ERROR": "timeout.md",
    "BANK_UNAVAILABLE": "bank_unavailable.md",
    "PAYMENT_METHOD_ISSUE": "payment_method_issue.md",
    "INSUFFICIENT_FUNDS": "payment_method_issue.md",
    "ABANDONED": "payment_method_issue.md",
    "PERMANENT_FAILURE": "permanent_failure.md",
}


def retrieve_policy(failure_reason: str) -> tuple[str, str]:
    """
    Retrieve the recovery policy relevant to a payment failure.

    Returns:
        (policy_id, policy_content)
    """

    filename = POLICY_MAPPING.get(
        failure_reason.upper(),
        "permanent_failure.md",
    )

    policy_path = POLICY_DIRECTORY / filename

    if not policy_path.exists():
        raise FileNotFoundError(
            f"Recovery policy not found: {filename}"
        )

    content = policy_path.read_text(encoding="utf-8")

    policy_id = "UNKNOWN"

    for line in content.splitlines():
        if line.startswith("Policy ID"):
            policy_id = line.split(":", 1)[1].strip()
            break

    return policy_id, content