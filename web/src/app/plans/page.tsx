"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Sparkles, Plus, Trash2, Edit, RefreshCw } from "lucide-react";

interface Plan {
  id: string;
  title: string;
  price?: number;
  coins?: number;
  durationDays?: number;
  description?: string;
  badge?: string;
  isActive?: boolean;
}

export default function PlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [price, setPrice] = useState(0);
  const [coins, setCoins] = useState(0);
  const [durationDays, setDurationDays] = useState(30);
  const [description, setDescription] = useState("");
  const [badge, setBadge] = useState("");

  useEffect(() => {
    fetchPlans();
  }, []);

  const fetchPlans = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "plans"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Plan[];
      setPlans(list);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    const docRef = editingId ? doc(db, "plans", editingId) : doc(collection(db, "plans"));
    await setDoc(docRef, {
      title,
      price: Number(price),
      coins: Number(coins),
      durationDays: Number(durationDays),
      description,
      badge,
      isActive: true,
      updatedAt: new Date().toISOString(),
    }, { merge: true });

    setShowModal(false);
    fetchPlans();
  };

  const handleDelete = async (id: string) => {
    if (confirm("Delete this subscription plan?")) {
      await deleteDoc(doc(db, "plans", id));
      fetchPlans();
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Subscription Plans</h1>
          <p className="text-slate-500 text-sm mt-1">Manage coin packages and premium subscription plans</p>
        </div>
        <button
          onClick={() => {
            setEditingId(null);
            setTitle("");
            setPrice(0);
            setCoins(0);
            setDurationDays(30);
            setDescription("");
            setBadge("");
            setShowModal(true);
          }}
          className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
        >
          <Plus className="w-4 h-4" /> Add Plan
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading plans...
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {plans.map((p) => (
            <div key={p.id} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4 relative">
              {p.badge && (
                <span className="absolute top-4 right-4 bg-indigo-100 text-indigo-700 text-xs font-bold px-2.5 py-0.5 rounded-full">
                  {p.badge}
                </span>
              )}
              <h3 className="font-bold text-slate-900 text-xl">{p.title}</h3>
              <p className="text-3xl font-extrabold text-indigo-600">
                Rs. {p.price ?? 0}
              </p>
              <div className="text-sm text-slate-600 space-y-1">
                <p>🪙 <strong>{p.coins ?? 0}</strong> Coins</p>
                <p>⏳ Duration: <strong>{p.durationDays ?? 30} Days</strong></p>
                {p.description && <p className="text-xs text-slate-400 pt-2 border-t">{p.description}</p>}
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t">
                <button
                  onClick={() => {
                    setEditingId(p.id);
                    setTitle(p.title);
                    setPrice(p.price || 0);
                    setCoins(p.coins || 0);
                    setDurationDays(p.durationDays || 30);
                    setDescription(p.description || "");
                    setBadge(p.badge || "");
                    setShowModal(true);
                  }}
                  className="p-1.5 text-slate-400 hover:text-indigo-600"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button onClick={() => handleDelete(p.id)} className="p-1.5 text-slate-400 hover:text-rose-600">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}

          {plans.length === 0 && (
            <p className="col-span-full py-12 text-center text-slate-400">No subscription plans found.</p>
          )}
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">
              {editingId ? "Edit Plan" : "Add Plan"}
            </h3>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Plan Title</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Starter Pack, VIP Pro"
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Price (PKR)</label>
                  <input
                    type="number"
                    required
                    value={price}
                    onChange={(e) => setPrice(Number(e.target.value))}
                    className="w-full border rounded-xl px-4 py-2.5 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Coins Included</label>
                  <input
                    type="number"
                    required
                    value={coins}
                    onChange={(e) => setCoins(Number(e.target.value))}
                    className="w-full border rounded-xl px-4 py-2.5 text-sm"
                  />
                </div>
              </div>
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-sm bg-indigo-600 text-white font-medium rounded-xl shadow-md"
                >
                  Save Plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
