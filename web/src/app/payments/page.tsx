"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { CreditCard, Check, X, RefreshCw } from "lucide-react";

interface Withdrawal {
  id: string;
  userId?: string;
  userName?: string;
  amount?: number;
  paymentMethod?: string;
  accountDetails?: string;
  status?: string; // "pending" | "approved" | "rejected"
  createdAt?: string;
}

export default function PaymentsPage() {
  const [requests, setRequests] = useState<Withdrawal[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchWithdrawals();
  }, []);

  const fetchWithdrawals = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "withdrawals"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Withdrawal[];
      setRequests(list);
    } catch (err) {
      console.error("Error fetching withdrawals:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id: string, newStatus: "approved" | "rejected") => {
    await updateDoc(doc(db, "withdrawals", id), { status: newStatus });
    fetchWithdrawals();
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Payments & Withdrawals</h1>
          <p className="text-slate-500 text-sm mt-1">Review user withdrawal and reward payout requests</p>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading payment requests...
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 border-b border-slate-200 uppercase text-xs text-slate-500 font-semibold">
              <tr>
                <th className="py-3.5 px-6">User / Account</th>
                <th className="py-3.5 px-6">Amount</th>
                <th className="py-3.5 px-6">Payment Method</th>
                <th className="py-3.5 px-6">Status</th>
                <th className="py-3.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {requests.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/50">
                  <td className="py-4 px-6">
                    <div className="font-semibold text-slate-900">{r.userName || r.userId || "User"}</div>
                    <div className="text-xs text-slate-400">{r.accountDetails || "No account details"}</div>
                  </td>
                  <td className="py-4 px-6 font-bold text-emerald-600">
                    Rs. {r.amount ?? 0}
                  </td>
                  <td className="py-4 px-6 font-medium text-slate-700">
                    {r.paymentMethod || "Easypaisa / Jazzcash"}
                  </td>
                  <td className="py-4 px-6">
                    {r.status === "approved" ? (
                      <span className="bg-emerald-100 text-emerald-800 text-xs px-2.5 py-1 rounded-full font-semibold">
                        Approved
                      </span>
                    ) : r.status === "rejected" ? (
                      <span className="bg-rose-100 text-rose-800 text-xs px-2.5 py-1 rounded-full font-semibold">
                        Rejected
                      </span>
                    ) : (
                      <span className="bg-amber-100 text-amber-800 text-xs px-2.5 py-1 rounded-full font-semibold">
                        Pending
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-6 text-right space-x-2">
                    {r.status !== "approved" && (
                      <button
                        onClick={() => handleUpdateStatus(r.id, "approved")}
                        className="bg-emerald-600 hover:bg-emerald-500 text-white p-1.5 rounded-lg text-xs font-semibold inline-flex items-center gap-1 shadow-sm"
                      >
                        <Check className="w-4 h-4" /> Approve
                      </button>
                    )}
                    {r.status !== "rejected" && (
                      <button
                        onClick={() => handleUpdateStatus(r.id, "rejected")}
                        className="bg-rose-600 hover:bg-rose-500 text-white p-1.5 rounded-lg text-xs font-semibold inline-flex items-center gap-1 shadow-sm"
                      >
                        <X className="w-4 h-4" /> Reject
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {requests.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    No payment requests found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
