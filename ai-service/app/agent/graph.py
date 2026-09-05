from langgraph.graph import END, START, StateGraph

from app.agent.nodes import (
    classify_failure,
    decide_recovery_action,
    retrieve_recovery_policy,
)
from app.agent.state import RecoveryState


def build_recovery_graph():
    """
    Build the RazorRecover AI recovery workflow.
    """

    graph = StateGraph(RecoveryState)

    # --------------------------------------------------------
    # Nodes
    # --------------------------------------------------------

    graph.add_node(
        "classify_failure",
        classify_failure,
    )

    graph.add_node(
        "retrieve_policy",
        retrieve_recovery_policy,
    )

    graph.add_node(
        "decide_action",
        decide_recovery_action,
    )

    # --------------------------------------------------------
    # Graph Flow
    # --------------------------------------------------------

    graph.add_edge(
        START,
        "classify_failure",
    )

    graph.add_edge(
        "classify_failure",
        "retrieve_policy",
    )

    graph.add_edge(
        "retrieve_policy",
        "decide_action",
    )

    graph.add_edge(
        "decide_action",
        END,
    )

    return graph.compile()


recovery_graph = build_recovery_graph()