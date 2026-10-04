"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, deleteDoc, updateDoc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Users, Search, Trash2, Edit, RefreshCw } from "lucide-react";

interface UserProfile {
  id: string;
  name?: string;
  email?: string;
  coins?: number;
  points?: number;
  lifelines?: {
    f5050?: number;
    audience?: number;
    skip?: number;
    resetTimer?: number;
  };
  blocked?: boolean;
  isPremium?: boolean;
  createdAt?: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  // Edit Modal
  const [showModal, setShowModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);

  const [editName, setEditName] = useState("");
  const [editCoins, setEditCoins] = useState(0);
  const [editPoints, setEditPoints] = useState(0);
  const [edit5050, setEdit5050] = useState(0);
  const [editAudience, setEditAudience] = useState(0);
  const [editSkip, setEditSkip] = useState(0);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "users"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as UserProfile[];
      setUsers(list);
    } catch (err) {
      console.error("Error fetching users:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEdit = (user: UserProfile) => {
    setSelectedUser(user);
    setEditName(user.name || "");
    setEditCoins(user.coins || 0);
    setEditPoints(user.points || 0);
    setEdit5050(user.lifelines?.f5050 || 3);
    setEditAudience(user.lifelines?.audience || 3);
    setEditSkip(user.lifelines?.skip || 3);
    setShowModal(true);
  };

  const handleSaveUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;

    try {
      await updateDoc(doc(db, "users", selectedUser.id), {
        name: editName,
        coins: Number(editCoins),
        points: Number(editPoints),
        lifelines: {
          f5050: Number(edit5050),
          audience: Number(editAudience),
          skip: Number(editSkip),
        },
        updatedAt: new Date().toISOString(),
      });

      setShowModal(false);
      fetchUsers();
    } catch (err) {
      console.error(err);
    }
  };

  const handleToggleBlock = async (user: UserProfile) => {
    const nextState = !user.blocked;
    await updateDoc(doc(db, "users", user.id), { blocked: nextState });
    fetchUsers();
  };

  const handleDelete = async (id: string) => {
    if (confirm("Delete this user profile permanently?")) {
      await deleteDoc(doc(db, "users", id));
      fetchUsers();
    }
  };

  const filteredUsers = users.filter(u =>
    u.name?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.id.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Users & Lifelines</h1>
          <p className="text-slate-500 text-sm mt-1">Manage user coins, lifelines, premium status, and block access</p>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search users by name, email or UID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading users...
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 border-b border-slate-200 uppercase text-xs text-slate-500 font-semibold">
              <tr>
                <th className="py-3.5 px-6">User</th>
                <th className="py-3.5 px-6">Coins</th>
                <th className="py-3.5 px-6">Points</th>
                <th className="py-3.5 px-6">Lifelines (50-50/Poll/Skip)</th>
                <th className="py-3.5 px-6">Status</th>
                <th className="py-3.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredUsers.map((u) => (
                <tr key={u.id} className="hover:bg-slate-50/50">
                  <td className="py-4 px-6">
                    <div className="font-semibold text-slate-900">{u.name || "Anonymous User"}</div>
                    <div className="text-xs text-slate-400">{u.email || u.id}</div>
                  </td>
                  <td className="py-4 px-6 font-bold text-amber-600">
                    🪙 {u.coins ?? 0}
                  </td>
                  <td className="py-4 px-6 font-semibold text-indigo-600">
                    🏆 {u.points ?? 0}
                  </td>
                  <td className="py-4 px-6 text-xs text-slate-600 font-mono">
                    {u.lifelines?.f5050 ?? 3} / {u.lifelines?.audience ?? 3} / {u.lifelines?.skip ?? 3}
                  </td>
                  <td className="py-4 px-6">
                    {u.blocked ? (
                      <span className="bg-rose-100 text-rose-800 text-xs px-2.5 py-1 rounded-full font-semibold">
                        Blocked
                      </span>
                    ) : (
                      <span className="bg-emerald-100 text-emerald-800 text-xs px-2.5 py-1 rounded-full font-semibold">
                        Active
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-6 text-right space-x-2">
                    <button
                      onClick={() => handleOpenEdit(u)}
                      className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-indigo-50"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleToggleBlock(u)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${
                        u.blocked ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
                      }`}
                    >
                      {u.blocked ? "Unblock" : "Block"}
                    </button>
                    <button
                      onClick={() => handleDelete(u.id)}
                      className="p-1.5 text-rose-500 hover:bg-rose-50 rounded-lg"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Edit User Modal */}
      {showModal && selectedUser && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-slate-900">Edit User Details</h3>
            <form onSubmit={handleSaveUser} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Name</label>
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full border rounded-xl px-4 py-2 text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Coins</label>
                  <input
                    type="number"
                    value={editCoins}
                    onChange={(e) => setEditCoins(Number(e.target.value))}
                    className="w-full border rounded-xl px-4 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Points</label>
                  <input
                    type="number"
                    value={editPoints}
                    onChange={(e) => setEditPoints(Number(e.target.value))}
                    className="w-full border rounded-xl px-4 py-2 text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Lifelines Count</label>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <span className="text-[10px] uppercase text-slate-400">50:50</span>
                    <input
                      type="number"
                      value={edit5050}
                      onChange={(e) => setEdit5050(Number(e.target.value))}
                      className="w-full border rounded-xl px-3 py-1.5 text-sm"
                    />
                  </div>
                  <div>
                    <span className="text-[10px] uppercase text-slate-400">Audience</span>
                    <input
                      type="number"
                      value={editAudience}
                      onChange={(e) => setEditAudience(Number(e.target.value))}
                      className="w-full border rounded-xl px-3 py-1.5 text-sm"
                    />
                  </div>
                  <div>
                    <span className="text-[10px] uppercase text-slate-400">Skip</span>
                    <input
                      type="number"
                      value={editSkip}
                      onChange={(e) => setEditSkip(Number(e.target.value))}
                      className="w-full border rounded-xl px-3 py-1.5 text-sm"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
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
                  Save User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
