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