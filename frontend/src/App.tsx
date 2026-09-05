import { useEffect, useMemo, useState } from "react";
import RazorpayCheckout from "./components/RazorpayCheckout";
import {
  createPayment,
  createRazorpayOrder,
  getPayments,
  getRecoveryCases,
  getRecoveryDecisions,
  runEvaluation,
  verifyRazorpayPayment,
  type Payment,
  type RazorpayOrder,
  type RecoveryCase,
  type RecoveryDecision,
  type EvaluationMetrics,
} from "./api";

function App() {
  const [payment, setPayment] = useState<Payment | null>(null);
  const [razorpayOrder, setRazorpayOrder] =
    useState<RazorpayOrder | null>(null);

  const [payments, setPayments] = useState<Payment[]>([]);
  const [recoveryCases, setRecoveryCases] = useState<RecoveryCase[]>([]);
  const [selectedCase, setSelectedCase] =
    useState<RecoveryCase | null>(null);
  const [decisions, setDecisions] = useState<RecoveryDecision[]>([]);

  const [loading, setLoading] = useState(false);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [message, setMessage] = useState("");

  const [evaluation, setEvaluation] =
    useState<EvaluationMetrics | null>(null);

  const [evaluationLoading, setEvaluationLoading] =
    useState(false);

  const loadDashboard = async () => {
    try {
      setDashboardLoading(true);

      const [paymentData, caseData] = await Promise.all([
        getPayments(),
        getRecoveryCases(),
      ]);

      setPayments(paymentData);
      setRecoveryCases(caseData);
    } catch (error) {
      console.error("Failed to load dashboard:", error);
    } finally {
      setDashboardLoading(false);
    }
  };

  const loadEvaluation = async () => {
    try {
      setEvaluationLoading(true);

      const metrics = await runEvaluation(1000, 42);

      setEvaluation(metrics);
    } catch (error) {
      console.error("Failed to load evaluation:", error);
    } finally {
      setEvaluationLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
    loadEvaluation();
  }, []);

  const selectRecoveryCase = async (recoveryCase: RecoveryCase) => {
    setSelectedCase(recoveryCase);

    try {
      const decisionData = await getRecoveryDecisions(
        recoveryCase.id
      );

      setDecisions(decisionData);
    } catch (error) {
      console.error("Failed to load recovery decisions:", error);
      setDecisions([]);
    }
  };

  const startPayment = async () => {
    try {
      setLoading(true);
      setMessage("");
      setRazorpayOrder(null);

      const createdPayment = await createPayment(250);

      setPayment(createdPayment);

      const order = await createRazorpayOrder(createdPayment.id);

      setRazorpayOrder(order);

      setMessage("Razorpay Order created. Ready for Checkout.");

      await loadDashboard();
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Failed to create payment"
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePaymentSuccess = async (response: {
    razorpay_payment_id: string;
    razorpay_order_id: string;
    razorpay_signature: string;
  }) => {
    if (!payment) {
      setMessage("Payment record is missing.");
      return;
    }

    try {
      setLoading(true);
      setMessage("Verifying payment...");

      const verifiedPayment = await verifyRazorpayPayment(
        payment.id,
        {
          razorpayOrderId: response.razorpay_order_id,
          razorpayPaymentId: response.razorpay_payment_id,
          razorpaySignature: response.razorpay_signature,
        }
      );

      setPayment(verifiedPayment);

      setMessage(
        "Payment verified successfully! Revenue recovered."
      );

      await loadDashboard();
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Payment verification failed"
      );
    } finally {
      setLoading(false);
    }
  };

  const handlePaymentFailure = (error: unknown) => {
    console.error(error);
    setMessage(
      "Razorpay payment failed or was cancelled."
    );
  };

  const totalPayments = payments.length;

  const failedPayments = payments.filter(
    (item) => item.status === "FAILED"
  ).length;

  const recoveredCases = recoveryCases.filter(
    (item) => item.status === "RECOVERED"
  );

  const escalatedCases = recoveryCases.filter(
    (item) => item.status === "ESCALATED"
  );

  const revenueAtRisk = useMemo(() => {
    return payments
      .filter((item) => item.status === "FAILED")
      .reduce(
        (total, item) => total + item.amount,
        0
      );
  }, [payments]);

  const recoveredRevenue = useMemo(() => {
    return recoveredCases.reduce(
      (total, recoveryCase) => {
        const matchingPayment = payments.find(
          (item) => item.id === recoveryCase.paymentId
        );

        return (
          total +
          (matchingPayment?.amount ?? 0)
        );
      },
      0
    );
  }, [recoveredCases, payments]);

  const recoveryRate =
    failedPayments > 0
      ? Math.round(
          (recoveredCases.length / failedPayments) * 100
        )
      : 0;

  const recentCases = [...recoveryCases]
    .sort(
      (a, b) =>
        new Date(b.openedAt).getTime() -
        new Date(a.openedAt).getTime()
    )
    .slice(0, 8);

  const selectedPayment = selectedCase
    ? payments.find(
        (item) => item.id === selectedCase.paymentId
      )
    : null;

  const latestDecision =
    decisions.length > 0
      ? [...decisions].sort(
          (a, b) =>
            new Date(b.createdAt).getTime() -
            new Date(a.createdAt).getTime()
        )[0]
      : null;

  const formatAmount = (
    amount: number,
    currency = "INR"
  ) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString(
      "en-IN",
      {
        dateStyle: "medium",
        timeStyle: "short",
      }
    );
  };

  const statusClass = (status: string) => {
    switch (status) {
      case "RECOVERED":
        return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";

      case "ESCALATED":
        return "bg-orange-500/10 text-orange-400 border-orange-500/20";

      case "STOPPED":
        return "bg-red-500/10 text-red-400 border-red-500/20";

      case "RETRY_PENDING":
        return "bg-blue-500/10 text-blue-400 border-blue-500/20";

      default:
        return "bg-yellow-500/10 text-yellow-400 border-yellow-500/20";
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl">

        {/* Header */}
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex items-center gap-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 font-bold">
                RR
              </div>

              <div>
                <p className="text-sm font-semibold uppercase tracking-widest text-blue-400">
                  RazorRecover
                </p>

                <h1 className="text-2xl font-bold sm:text-3xl">
                  AI Revenue Recovery
                </h1>
              </div>

            </div>

            <p className="mt-3 text-sm text-slate-400">
              Autonomous payment recovery with AI decisions
              and deterministic safety controls.
            </p>
          </div>

          <div className="flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-400">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            System Live
          </div>
        </header>

        {/* Metrics */}
        <section className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">

          <MetricCard
            label="Revenue at Risk"
            value={formatAmount(revenueAtRisk)}
            description={`${failedPayments} failed payments`}
          />

          <MetricCard
            label="Recovered Revenue"
            value={formatAmount(recoveredRevenue)}
            description={`${recoveredCases.length} recovered cases`}
          />

          <MetricCard
            label="Recovery Rate"
            value={`${recoveryRate}%`}
            description="Failed payment recovery"
          />

          <MetricCard
            label="Total Payments"
            value={totalPayments.toString()}
            description={`${escalatedCases.length} escalated`}
          />

        </section>

        {/* AI Revenue Impact */}
        <section className="mb-8 rounded-2xl border border-blue-500/20 bg-slate-900 p-6 shadow-xl">

          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-blue-400">
                AI Performance Evaluation
              </p>

              <h2 className="mt-2 text-xl font-semibold">
                AI Revenue Impact
              </h2>

              <p className="mt-1 text-sm text-slate-400">
                Deterministic benchmark comparing traditional
                recovery with RazorRecover AI.
              </p>
            </div>

            <button
              type="button"
              onClick={loadEvaluation}
              disabled={evaluationLoading}
              className="rounded-lg border border-blue-500/30 px-4 py-2 text-sm font-medium text-blue-400 transition hover:bg-blue-500/10 disabled:opacity-50"
            >
              {evaluationLoading
                ? "Running..."
                : "Run Evaluation"}
            </button>

          </div>

          {evaluationLoading && !evaluation && (
            <div className="mt-6 rounded-xl border border-slate-800 bg-slate-950 p-6 text-center text-sm text-slate-500">
              Running 1,000-payment revenue recovery benchmark...
            </div>
          )}

          {evaluation && (
            <div className="mt-6">

              {/* Primary impact */}
              <div className="mb-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-5">

                <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">

                  <div>
                    <p className="text-xs uppercase tracking-wider text-slate-500">
                      Incremental Revenue Recovered
                    </p>

                    <p className="mt-2 text-3xl font-bold text-emerald-400">
                      {formatAmount(
                        evaluation.incrementalRecoveredRevenue
                      )}
                    </p>
                  </div>

                  <div className="text-left sm:text-right">
                    <p className="text-xs uppercase tracking-wider text-slate-500">
                      Improvement
                    </p>

                    <p className="mt-2 text-2xl font-bold text-emerald-400">
                      +{evaluation.recoveryImprovementPercent}%
                    </p>
                  </div>

                </div>

              </div>

              {/* Evaluation metrics */}
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">

                <EvaluationCard
                  label="Dataset"
                  value={evaluation.datasetSize.toLocaleString("en-IN")}
                  description="synthetic failed payments"
                />

                <EvaluationCard
                  label="Revenue at Risk"
                  value={formatAmount(
                    evaluation.revenueAtRisk
                  )}
                  description="total payment value"
                />

                <EvaluationCard
                  label="Baseline"
                  value={formatAmount(
                    evaluation.baselineRecoveredRevenue
                  )}
                  description={`${evaluation.baselineRecoveryRate}% recovery`}
                />

                <EvaluationCard
                  label="AI Recovery"
                  value={formatAmount(
                    evaluation.aiRecoveredRevenue
                  )}
                  description={`${evaluation.aiRecoveryRate}% recovery`}
                />

                <EvaluationCard
                  label="Escalation"
                  value={`${evaluation.escalationRate}%`}
                  description="safely escalated"
                />

              </div>

            </div>
          )}

        </section>

        {/* Main grid */}
        <div className="grid gap-6 lg:grid-cols-[1.35fr_0.65fr]">

          {/* Recovery cases */}
          <section className="rounded-2xl border border-slate-800 bg-slate-900 shadow-xl">

            <div className="flex items-center justify-between border-b border-slate-800 px-6 py-5">

              <div>
                <h2 className="text-lg font-semibold">
                  Recovery Cases
                </h2>

                <p className="mt-1 text-sm text-slate-400">
                  Recent payment recovery activity
                </p>
              </div>

              <button
                type="button"
                onClick={loadDashboard}
                disabled={dashboardLoading}
                className="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800 disabled:opacity-50"
              >
                {dashboardLoading
                  ? "Refreshing..."
                  : "Refresh"}
              </button>

            </div>

            <div className="overflow-x-auto">

              <table className="w-full text-left text-sm">

                <thead className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">

                  <tr>
                    <th className="px-6 py-4">
                      Payment
                    </th>

                    <th className="px-6 py-4">
                      Amount
                    </th>

                    <th className="px-6 py-4">
                      Retries
                    </th>

                    <th className="px-6 py-4">
                      Status
                    </th>
                  </tr>

                </thead>

                <tbody>

                  {recentCases.map(
                    (recoveryCase) => {

                      const casePayment =
                        payments.find(
                          (item) =>
                            item.id ===
                            recoveryCase.paymentId
                        );

                      const isSelected =
                        selectedCase?.id ===
                        recoveryCase.id;

                      return (
                        <tr
                          key={recoveryCase.id}
                          onClick={() =>
                            selectRecoveryCase(
                              recoveryCase
                            )
                          }
                          className={`cursor-pointer border-b border-slate-800/70 transition hover:bg-slate-800/50 ${
                            isSelected
                              ? "bg-slate-800/70"
                              : ""
                          }`}
                        >

                          <td className="px-6 py-5">

                            <div>
                              <p className="font-medium text-white">
                                {casePayment?.failureReason ??
                                  "Payment recovery"}
                              </p>

                              <p className="mt-1 font-mono text-xs text-slate-500">
                                {recoveryCase.paymentId.slice(
                                  0,
                                  14
                                )}
                                ...
                              </p>
                            </div>

                          </td>

                          <td className="px-6 py-5 font-semibold">
                            {casePayment
                              ? formatAmount(
                                  casePayment.amount,
                                  casePayment.currency
                                )
                              : "—"}
                          </td>

                          <td className="px-6 py-5 text-slate-300">
                            {recoveryCase.retryCount}
                          </td>

                          <td className="px-6 py-5">

                            <span
                              className={`inline-flex rounded-full border px-3 py-1 text-xs font-medium ${statusClass(
                                recoveryCase.status
                              )}`}
                            >
                              {recoveryCase.status}
                            </span>

                          </td>

                        </tr>
                      );
                    }
                  )}

                  {!dashboardLoading &&
                    recentCases.length === 0 && (
                      <tr>
                        <td
                          colSpan={4}
                          className="px-6 py-12 text-center text-slate-500"
                        >
                          No recovery cases yet.
                        </td>
                      </tr>
                    )}

                </tbody>

              </table>

            </div>

          </section>

          {/* Selected recovery */}
          <section className="rounded-2xl border border-slate-800 bg-slate-900 shadow-xl">

            <div className="border-b border-slate-800 px-6 py-5">

              <h2 className="text-lg font-semibold">
                Recovery Intelligence
              </h2>

              <p className="mt-1 text-sm text-slate-400">
                AI decision and safety evaluation
              </p>

            </div>

            {!selectedCase && (
              <div className="flex min-h-[400px] items-center justify-center px-6 text-center">

                <div>

                  <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10 text-2xl">
                    AI
                  </div>

                  <p className="font-medium text-slate-300">
                    Select a recovery case
                  </p>

                  <p className="mt-2 text-sm text-slate-500">
                    Click a case to inspect its recovery decision.
                  </p>

                </div>

              </div>
            )}

            {selectedCase && (
              <div className="space-y-6 p-6">

                {/* Case */}
                <div>

                  <p className="text-xs uppercase tracking-wider text-slate-500">
                    Recovery Case
                  </p>

                  <p className="mt-2 break-all font-mono text-xs text-slate-400">
                    {selectedCase.id}
                  </p>

                  <div className="mt-4 flex items-center justify-between">

                    <span className="text-sm text-slate-400">
                      Status
                    </span>

                    <span
                      className={`rounded-full border px-3 py-1 text-xs font-medium ${statusClass(
                        selectedCase.status
                      )}`}
                    >
                      {selectedCase.status}
                    </span>

                  </div>

                </div>

                {/* Payment */}
                <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                  <p className="text-xs uppercase tracking-wider text-slate-500">
                    Payment
                  </p>

                  <div className="mt-3 grid grid-cols-2 gap-4">

                    <div>

                      <p className="text-xs text-slate-500">
                        Amount
                      </p>

                      <p className="mt-1 font-semibold">
                        {selectedPayment
                          ? formatAmount(
                              selectedPayment.amount,
                              selectedPayment.currency
                            )
                          : "—"}
                      </p>

                    </div>

                    <div>

                      <p className="text-xs text-slate-500">
                        Failure
                      </p>

                      <p className="mt-1 font-semibold text-orange-400">
                        {selectedPayment?.failureReason ??
                          "—"}
                      </p>

                    </div>

                  </div>

                </div>

                {/* AI decision */}
                {latestDecision && (
                  <div className="rounded-xl border border-blue-500/20 bg-blue-500/5 p-4">

                    <div className="flex items-center justify-between">

                      <p className="text-xs uppercase tracking-wider text-blue-400">
                        AI Recommendation
                      </p>

                      <span className="text-xs text-slate-500">
                        {(
                          latestDecision.confidence * 100
                        ).toFixed(0)}
                        % confidence
                      </span>

                    </div>

                    <p className="mt-3 text-lg font-semibold">
                      {latestDecision.recommendedAction}
                    </p>

                    <p className="mt-2 text-sm leading-6 text-slate-400">
                      {latestDecision.explanation}
                    </p>

                  </div>
                )}

                {/* Safety */}
                {latestDecision && (
                  <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                    <p className="text-xs uppercase tracking-wider text-slate-500">
                      Safety Gate
                    </p>

                    <div className="mt-3">

                      <p
                        className={`font-semibold ${
                          latestDecision.safetyCheckSummary.includes(
                            "PASSED"
                          )
                            ? "text-emerald-400"
                            : "text-red-400"
                        }`}
                      >
                        {latestDecision.safetyCheckSummary}
                      </p>

                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-4">

                      <div>

                        <p className="text-xs text-slate-500">
                          Final Action
                        </p>

                        <p className="mt-1 font-semibold">
                          {latestDecision.finalAction}
                        </p>

                      </div>

                      <div>

                        <p className="text-xs text-slate-500">
                          Outcome
                        </p>

                        <p className="mt-1 font-semibold text-emerald-400">
                          {latestDecision.outcome}
                        </p>

                      </div>

                    </div>

                  </div>
                )}

                {!latestDecision && (
                  <div className="rounded-xl border border-slate-800 bg-slate-950 p-4 text-sm text-slate-500">
                    No AI decision recorded for this case yet.
                  </div>
                )}

                {/* Timeline */}
                <div>

                  <p className="text-xs uppercase tracking-wider text-slate-500">
                    Recovery Timeline
                  </p>

                  <div className="mt-4 space-y-4">

                    <TimelineItem
                      title="Payment failure detected"
                      description={
                        selectedPayment?.failureReason ??
                        "Payment failed"
                      }
                    />

                    <TimelineItem
                      title="Recovery case created"
                      description={formatDate(
                        selectedCase.openedAt
                      )}
                    />

                    {latestDecision && (
                      <TimelineItem
                        title="AI recovery decision"
                        description={
                          latestDecision.recommendedAction
                        }
                      />
                    )}

                    {latestDecision && (
                      <TimelineItem
                        title="Safety gate evaluated"
                        description={
                          latestDecision.safetyCheckSummary
                        }
                      />
                    )}

                    {selectedCase.status ===
                      "RECOVERED" && (
                      <TimelineItem
                        title="Revenue recovered"
                        description="Payment successfully recovered"
                        success
                      />
                    )}

                  </div>

                </div>

              </div>
            )}

          </section>

        </div>

        {/* Razorpay Demo */}
        <section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-xl">

          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

            <div>

              <p className="text-xs font-semibold uppercase tracking-wider text-blue-400">
                Live Payment Demo
              </p>

              <h2 className="mt-2 text-xl font-semibold">
                Test Razorpay Checkout
              </h2>

              <p className="mt-2 max-w-2xl text-sm text-slate-400">
                Create a real Razorpay Test Mode order and
                verify the payment through the backend.
              </p>

            </div>

            <div className="rounded-full bg-yellow-500/10 px-4 py-2 text-sm font-medium text-yellow-400">
              Test Mode
            </div>

          </div>

          <div className="mt-6 border-t border-slate-800 pt-6">

            {!payment && (
              <button
                type="button"
                onClick={startPayment}
                disabled={loading}
                className="rounded-lg bg-blue-600 px-6 py-3 font-semibold transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {loading
                  ? "Creating Order..."
                  : "Start Razorpay Payment"}
              </button>
            )}

            {payment && (
              <div className="space-y-5">

                <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

                    <div>

                      <p className="text-xs uppercase tracking-wider text-slate-500">
                        Internal Payment
                      </p>

                      <p className="mt-2 break-all font-mono text-xs text-slate-400">
                        {payment.id}
                      </p>

                    </div>

                    <span
                      className={`rounded-full border px-3 py-1 text-xs font-medium ${statusClass(
                        payment.status
                      )}`}
                    >
                      {payment.status}
                    </span>

                  </div>

                </div>

                {razorpayOrder && (
                  <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                    <p className="text-xs uppercase tracking-wider text-slate-500">
                      Razorpay Order
                    </p>

                    <p className="mt-2 break-all font-mono text-xs text-slate-400">
                      {razorpayOrder.orderId}
                    </p>

                    <div className="mt-5">

                      <RazorpayCheckout
                        orderId={razorpayOrder.orderId}
                        amount={razorpayOrder.amount}
                        currency={razorpayOrder.currency}
                        onSuccess={handlePaymentSuccess}
                        onFailure={handlePaymentFailure}
                      />

                    </div>

                  </div>
                )}

              </div>
            )}

            {message && (
              <div className="mt-5 rounded-lg border border-slate-700 bg-slate-950 p-4 text-sm text-slate-300">
                {message}
              </div>
            )}

          </div>

        </section>

        {/* Footer */}
        <footer className="mt-8 pb-4 text-center text-xs text-slate-600">
          RazorRecover · AI recommends · Policy retrieves · Java
          safety layer decides
        </footer>

      </div>
    </div>
  );
}

function MetricCard({
  label,
  value,
  description,
}: {
  label: string;
  value: string;
  description: string;
}) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5 shadow-xl">

      <p className="text-sm text-slate-400">
        {label}
      </p>

      <p className="mt-2 text-2xl font-bold tracking-tight">
        {value}
      </p>

      <p className="mt-2 text-xs text-slate-500">
        {description}
      </p>

    </div>
  );
}

function EvaluationCard({
  label,
  value,
  description,
}: {
  label: string;
  value: string;
  description: string;
}) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

      <p className="text-xs uppercase tracking-wider text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-lg font-bold text-white">
        {value}
      </p>

      <p className="mt-1 text-xs text-slate-500">
        {description}
      </p>

    </div>
  );
}

function TimelineItem({
  title,
  description,
  success = false,
}: {
  title: string;
  description: string;
  success?: boolean;
}) {
  return (
    <div className="flex gap-3">

      <div
        className={`mt-1 h-2.5 w-2.5 shrink-0 rounded-full ${
          success
            ? "bg-emerald-400"
            : "bg-blue-400"
        }`}
      />

      <div>

        <p className="text-sm font-medium text-slate-300">
          {title}
        </p>

        <p className="mt-1 text-xs leading-5 text-slate-500">
          {description}
        </p>

      </div>

    </div>
  );
}

export default App;