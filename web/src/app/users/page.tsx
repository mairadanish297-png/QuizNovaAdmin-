"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, doc, deleteDoc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Users, Search, Trash2, Shield, RefreshCw } from "lucide-react";

interface UserProfile {
  id: string;
  name?: string;
  email?: string;
  coins?: number;
  points?: number;
  blocked?: boolean;
  createdAt?: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

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

  const handleToggleBlock = async (user: UserProfile) => {
    const nextState = !user.blocked;
    await updateDoc(doc(db, "users", user.id), { blocked: nextState });
    fetchUsers();
  };

  const handleDelete = async (id: string) => {
    if (confirm("Delete this user profile?")) {
      await deleteDoc(doc(db, "users", id));
      fetchUsers();
    }
  };

  const filteredUsers = users.filter(u =>
    u.name?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Users Management</h1>
          <p className="text-slate-500 text-sm mt-1">View registered app users, coins, and block status</p>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search users by name or email..."
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
                <th className="py-3.5 px-6">Coins / Points</th>
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
                  <td className="py-4 px-6 font-medium text-slate-800">
                    🪙 {u.coins ?? 0} Coins
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
                  <td colSpan={4} className="py-8 text-center text-slate-400">
                    No users found.
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
