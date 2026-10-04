"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, setDoc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Trophy, Plus, Trash2, Edit, RefreshCw } from "lucide-react";

interface Tournament {
  id: string;
  title: string;
  entryFee?: number;
  prizePool?: number;
  startDate?: string;
  endDate?: string;
  category?: string;
  status?: string;
}

export default function TournamentsPage() {
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [loading, setLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [entryFee, setEntryFee] = useState(10);
  const [prizePool, setPrizePool] = useState(500);
  const [category, setCategory] = useState("General");
  const [status, setStatus] = useState("active");

  useEffect(() => {
    fetchTournaments();
  }, []);

  const fetchTournaments = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "tournaments"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as Tournament[];
      setTournaments(list);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    const docRef = editingId ? doc(db, "tournaments", editingId) : doc(collection(db, "tournaments"));
    await setDoc(docRef, {
      title,
      entryFee: Number(entryFee),
      prizePool: Number(prizePool),
      category,
      status,
      updatedAt: new Date().toISOString(),
    }, { merge: true });

    setShowModal(false);
    fetchTournaments();
  };

  const handleDelete = async (id: string) => {
    if (confirm("Delete this tournament?")) {
      await deleteDoc(doc(db, "tournaments", id));
      fetchTournaments();
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Tournaments & Battles</h1>
          <p className="text-slate-500 text-sm mt-1">Manage competitive quiz tournaments and prize pools</p>
        </div>
        <button
          onClick={() => {
            setEditingId(null);
            setTitle("");
            setEntryFee(10);
            setPrizePool(500);
            setCategory("General");
            setStatus("active");
            setShowModal(true);
          }}
          className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium px-4 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-2 text-sm"
        >
          <Plus className="w-4 h-4" /> Create Tournament
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading tournaments...
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tournaments.map((t) => (
            <div key={t.id} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center justify-between">
                <Trophy className="w-6 h-6 text-amber-500" />
                <span className="text-xs font-semibold bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded-full">
                  {t.status || "active"}
                </span>
              </div>

              <h3 className="font-bold text-slate-900 text-lg">{t.title}</h3>

              <div className="text-sm text-slate-600 space-y-1">
                <p>🏆 Prize Pool: <strong className="text-emerald-600">Rs. {t.prizePool ?? 0}</strong></p>
                <p>🎟️ Entry Fee: <strong>{t.entryFee ?? 0} Coins</strong></p>
                <p>📂 Category: <strong>{t.category || "General"}</strong></p>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t">
                <button
                  onClick={() => {
                    setEditingId(t.id);
                    setTitle(t.title);
                    setEntryFee(t.entryFee || 10);
                    setPrizePool(t.prizePool || 500);
                    setCategory(t.category || "General");
                    setStatus(t.status || "active");
                    setShowModal(true);
                  }}
                  className="p-1.5 text-slate-400 hover:text-indigo-600"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button onClick={() => handleDelete(t.id)} className="p-1.5 text-slate-400 hover:text-rose-600">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}

          {tournaments.length === 0 && (
            <p className="col-span-full py-12 text-center text-slate-400">No tournaments found.</p>
          )}
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">
              {editingId ? "Edit Tournament" : "Create Tournament"}
            </h3>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Title</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Mega Sunday Quiz Championship"
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Entry Fee (Coins)</label>
                  <input
                    type="number"
                    required
                    value={entryFee}
                    onChange={(e) => setEntryFee(Number(e.target.value))}
                    className="w-full border rounded-xl px-4 py-2.5 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Prize Pool (PKR)</label>
                  <input
                    type="number"
                    required
                    value={prizePool}
                    onChange={(e) => setPrizePool(Number(e.target.value))}
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
                  Save Tournament
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
