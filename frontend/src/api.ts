const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export interface Payment {
  id: string;
  amount: number;
  currency: string;
  status: string;
  failureReason?: string | null;
  scenario?: string | null;
}

export interface RazorpayOrder {
  orderId: string;
  amount: number;
  currency: "INR";
  keyId: string;
}

export interface RazorpayVerificationResponse {
  id: string;
  amount: number;
  currency: string;
  status: string;
  failureReason?: string | null;
}

export interface RecoveryCase {
  id: string;
  paymentId: string;
  correlationId: string;
  status: string;
  retryCount: number;
  openedAt: string;
  closedAt?: string | null;
}

export interface RecoveryDecision {
  id: string;
  recommendedAction: string;
  finalAction: string;
  confidence: number;
  explanation: string;
  safetyCheckSummary: string;
  outcome: string;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  actor: string;
  payload: string;
  occurredAt: string;
}

export interface EvaluationMetrics {
  datasetSize: number;
  revenueAtRisk: number;
  baselineRecoveredRevenue: number;
  aiRecoveredRevenue: number;
  baselineRecoveryRate: number;
  aiRecoveryRate: number;
  incrementalRecoveredRevenue: number;
  recoveryImprovementPercent: number;
  escalationRate: number;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));

    throw new Error(
      errorBody.message ||
        `Request failed with status ${response.status}`
    );
  }

  return response.json();
}

export async function createPayment(
  amount: number,
  currency = "INR",
  scenario = "SUCCESS"
): Promise<Payment> {
  const response = await fetch(`${API_BASE_URL}/api/payments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      amount,
      currency,
      scenario,
      processImmediately: false,
    }),
  });

  return handleResponse<Payment>(response);
}

export async function createRazorpayOrder(
  paymentId: string
): Promise<RazorpayOrder> {
  const response = await fetch(
    `${API_BASE_URL}/api/payments/${paymentId}/razorpay-order`,
    {
      method: "POST",
    }
  );

  return handleResponse<RazorpayOrder>(response);
}

export async function verifyRazorpayPayment(
  paymentId: string,
  data: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
  }
): Promise<RazorpayVerificationResponse> {
  const response = await fetch(
    `${API_BASE_URL}/api/payments/${paymentId}/razorpay/verify`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    }
  );

  return handleResponse<RazorpayVerificationResponse>(response);
}

export async function getPayments(): Promise<Payment[]> {
  const response = await fetch(`${API_BASE_URL}/api/payments`);

  return handleResponse<Payment[]>(response);
}

export async function getRecoveryCases(): Promise<RecoveryCase[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/recovery-cases`
  );

  return handleResponse<RecoveryCase[]>(response);
}

export async function getRecoveryDecisions(
  recoveryCaseId: string
): Promise<RecoveryDecision[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/recovery-cases/${recoveryCaseId}/decisions`
  );

  return handleResponse<RecoveryDecision[]>(response);
}

export async function getAuditEvents(
  recoveryCaseId: string
): Promise<AuditEvent[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/recovery-cases/${recoveryCaseId}/audit-events`
  );

  return handleResponse<AuditEvent[]>(response);
}

export async function runEvaluation(
  datasetSize = 1000,
  seed = 42
): Promise<EvaluationMetrics> {
  const response = await fetch(
    `${API_BASE_URL}/api/evaluation/run?datasetSize=${datasetSize}&seed=${seed}`,
    {
      method: "POST",
    }
  );

  return handleResponse<EvaluationMetrics>(response);
}