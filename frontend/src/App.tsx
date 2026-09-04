import { useState } from "react";
import RazorpayCheckout from "./components/RazorpayCheckout";
import {
  createPayment,
  createRazorpayOrder,
  verifyRazorpayPayment,
  type Payment,
  type RazorpayOrder,
} from "./api";

function App() {
  const [payment, setPayment] = useState<Payment | null>(null);
  const [razorpayOrder, setRazorpayOrder] = useState<RazorpayOrder | null>(
    null
  );
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const startPayment = async () => {
    try {
      setLoading(true);
      setMessage("");
      setRazorpayOrder(null);

      // 1. Create our internal payment.
      const createdPayment = await createPayment(250);

      setPayment(createdPayment);

      // 2. Ask our backend to create the Razorpay Order.
      const order = await createRazorpayOrder(createdPayment.id);

      setRazorpayOrder(order);

      setMessage("Razorpay Order created. Ready for Checkout.");
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
      setMessage("Payment verified successfully! Revenue recovered.");
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
    setMessage("Razorpay payment failed or was cancelled.");
  };

  return (
    <div className="min-h-screen bg-slate-950 px-6 py-12 text-white">
      <div className="mx-auto max-w-3xl">
        <div className="mb-10">
          <p className="mb-2 text-sm font-semibold uppercase tracking-widest text-blue-400">
            RazorRecover
          </p>

          <h1 className="text-4xl font-bold tracking-tight">
            AI Payment Recovery
          </h1>

          <p className="mt-3 text-slate-400">
            Test the complete Razorpay payment recovery flow.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl">
          <div className="mb-8 flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-400">Recovery Amount</p>
              <p className="mt-1 text-4xl font-bold">₹250.00</p>
            </div>

            <div className="rounded-full bg-yellow-500/10 px-4 py-2 text-sm font-medium text-yellow-400">
              Test Mode
            </div>
          </div>

          {!payment && (
            <button
              type="button"
              onClick={startPayment}
              disabled={loading}
              className="rounded-lg bg-blue-600 px-6 py-3 font-semibold transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? "Creating Order..." : "Start Payment Recovery"}
            </button>
          )}

          {payment && (
            <div className="space-y-6">
              <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">
                <p className="text-sm text-slate-400">Internal Payment ID</p>
                <p className="mt-1 break-all font-mono text-sm">
                  {payment.id}
                </p>

                <div className="mt-4">
                  <p className="text-sm text-slate-400">Status</p>
                  <p className="mt-1 font-semibold">{payment.status}</p>
                </div>
              </div>

              {razorpayOrder && (
                <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">
                  <p className="text-sm text-slate-400">
                    Razorpay Order ID
                  </p>

                  <p className="mt-1 break-all font-mono text-sm">
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
            <div className="mt-6 rounded-lg border border-slate-700 bg-slate-950 p-4 text-sm text-slate-300">
              {message}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;