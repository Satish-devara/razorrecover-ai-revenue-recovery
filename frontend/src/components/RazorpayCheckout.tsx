import { useState } from "react";
import { useRazorpay } from "react-razorpay";

interface RazorpayCheckoutProps {
  orderId: string;
  amount: number;
  currency: "INR";
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  onSuccess: (response: {
    razorpay_payment_id: string;
    razorpay_order_id: string;
    razorpay_signature: string;
  }) => void;
  onFailure?: (error: unknown) => void;
}

export default function RazorpayCheckout({
  orderId,
  amount,
  currency,
  customerName = "Test Customer",
  customerEmail = "test@example.com",
  customerPhone = "9999999999",
  onSuccess,
  onFailure,
}: RazorpayCheckoutProps) {
  const { Razorpay } = useRazorpay();
  const [loading, setLoading] = useState(false);

  const openCheckout = () => {
    setLoading(true);

    const options = {
      key: import.meta.env.VITE_RAZORPAY_KEY_ID,
      amount,
      currency,
      name: "RazorRecover",
      description: "Payment Recovery",
      order_id: orderId,

      prefill: {
        name: customerName,
        email: customerEmail,
        contact: customerPhone,
      },

      theme: {
        color: "#2563eb",
      },

      handler: (response: {
        razorpay_payment_id: string;
        razorpay_order_id: string;
        razorpay_signature: string;
      }) => {
        setLoading(false);
        onSuccess(response);
      },

      modal: {
        ondismiss: () => {
          setLoading(false);
        },
      },
    };

    const razorpay = new Razorpay(options);

    razorpay.on("payment.failed", (response) => {
      setLoading(false);

      console.error("Razorpay payment failed:", response);

      onFailure?.(response);
    });

    razorpay.open();
  };

  return (
    <button
      type="button"
      onClick={openCheckout}
      disabled={loading}
      className="rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
    >
      {loading ? "Opening Checkout..." : `Pay ₹${(amount / 100).toFixed(2)}`}
    </button>
  );
}