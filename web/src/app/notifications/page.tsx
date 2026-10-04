"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, addDoc, query, where } from "firebase/firestore";
import { db, auth } from "@/lib/firebase";
import { Bell, Send, Users, User, Crown, Clock, RefreshCw, CheckCircle2 } from "lucide-react";

interface NotificationHistoryItem {
  id: string;
  title: string;
  message: string;
  type: string;
  sentBy: string;
  createdAt?: string;
  imageUrl?: string;
}

export default function NotificationsPage() {
  const [targetType, setTargetType] = useState<"all" | "single" | "premium">("all");
  const [userId, setUserId] = useState("");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  const [sending, setSending] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [history, setHistory] = useState<NotificationHistoryItem[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(true);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoadingHistory(true);
    try {
      const snap = await getDocs(collection(db, "notifications"));
      const list = snap.docs.map(doc => ({ id: doc.id, ...doc.data() })) as NotificationHistoryItem[];
      setHistory(list.reverse());
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleSendNotification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !message.trim()) return;
    if (targetType === "single" && !userId.trim()) {
      alert("Please enter User ID for single user target.");
      return;
    }

    setSending(true);
    setSuccessMsg("");

    const adminEmail = auth.currentUser?.email || "admin@quiznova.com";
    const nowISO = new Date().toISOString();

    const notifPayload = {
      userId: targetType === "all" ? "all" : targetType === "premium" ? "premium" : userId,
      title,
      message,
      imageUrl,
      type: targetType,
      sentBy: adminEmail,
      isActive: true,
      createdAt: nowISO,
      timestamp: Date.now(),
    };

    try {
      // 1. Save to root 'notifications' collection
      await addDoc(collection(db, "notifications"), notifPayload);

      // 2. Write to individual users' notifications subcollections
      let userDocs: any[] = [];
      if (targetType === "all") {
        const uSnap = await getDocs(collection(db, "users"));
        userDocs = uSnap.docs;
      } else if (targetType === "single") {
        userDocs = [{ id: userId }];
      } else if (targetType === "premium") {
        const pSnap = await getDocs(query(collection(db, "users"), where("isPremium", "==", true)));
        userDocs = pSnap.docs;
      }

      let sentCount = 0;
      await Promise.all(userDocs.map(async (uDoc) => {
        try {
          await addDoc(collection(db, "users", uDoc.id, "notifications"), {
            title,
            message,
            imageUrl,
            type: targetType,
            read: false,
            sentBy: adminEmail,
            isActive: true,
            createdAt: nowISO,
            timestamp: Date.now(),
          });
          sentCount++;
        } catch (e) {
          console.error(`Failed to push to user ${uDoc.id}`, e);
        }
      }));

      setSuccessMsg(`Push Notification successfully dispatched to ${sentCount} user(s)!`);
      setTitle("");
      setMessage("");
      setImageUrl("");
      setUserId("");
      fetchHistory();
    } catch (err: any) {
      alert("Error sending notification: " + err.message);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-purple-600 text-white p-3 rounded-2xl shadow-lg">
            <Bell className="w-8 h-8" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Push Notifications</h1>
            <p className="text-slate-500 text-sm mt-0.5">Send real-time announcements & alerts to users</p>
          </div>
        </div>
      </div>

      {/* Notification Form */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
        {/* Target Tabs */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase mb-2">Target Audience</label>
          <div className="grid grid-cols-3 gap-3">
            <button
              type="button"
              onClick={() => setTargetType("all")}
              className={`p-3 rounded-xl border text-sm font-semibold flex items-center justify-center gap-2 transition-all ${
                targetType === "all"
                  ? "bg-purple-50 border-purple-500 text-purple-700 shadow-sm"
                  : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Users className="w-4 h-4" /> All Users
            </button>
            <button
              type="button"
              onClick={() => setTargetType("single")}
              className={`p-3 rounded-xl border text-sm font-semibold flex items-center justify-center gap-2 transition-all ${
                targetType === "single"
                  ? "bg-purple-50 border-purple-500 text-purple-700 shadow-sm"
                  : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
              }`}
            >
              <User className="w-4 h-4" /> Single User
            </button>
            <button
              type="button"
              onClick={() => setTargetType("premium")}
              className={`p-3 rounded-xl border text-sm font-semibold flex items-center justify-center gap-2 transition-all ${
                targetType === "premium"
                  ? "bg-amber-50 border-amber-500 text-amber-800 shadow-sm"
                  : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Crown className="w-4 h-4 text-amber-500" /> Premium Users
            </button>
          </div>
        </div>

        <form onSubmit={handleSendNotification} className="space-y-4">
          {targetType === "single" && (
            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Target User ID (UID)</label>
              <input
                type="text"
                required
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                placeholder="Paste Firebase User UID..."
                className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Notification Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. 🏆 New Quiz Contest Live Now!"
              className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Message Content</label>
            <textarea
              rows={3}
              required
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Enter full notification message..."
              className="w-full border rounded-xl p-3 text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Banner Image URL (Optional)</label>
            <input
              type="url"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://..."
              className="w-full border rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
            />
          </div>

          {successMsg && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-xl flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={sending}
            className="w-full bg-purple-600 hover:bg-purple-500 text-white font-medium py-3 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50 text-sm"
          >
            {sending ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" /> Dispatching Notification...
              </>
            ) : (
              <>
                <Send className="w-4 h-4" /> Send Push Notification
              </>
            )}
          </button>
        </form>
      </div>

      {/* Notification History */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
        <h2 className="text-xl font-bold text-slate-900 flex items-center justify-between">
          <span>Sent Notification History ({history.length})</span>
          <button onClick={fetchHistory} className="text-slate-400 hover:text-slate-600">
            <RefreshCw className="w-4 h-4" />
          </button>
        </h2>

        {loadingHistory ? (
          <div className="text-center py-8 text-slate-400 text-sm">Loading history...</div>
        ) : (
          <div className="space-y-3">
            {history.map((h) => (
              <div key={h.id} className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 text-sm">{h.title}</span>
                    <span className="text-xs uppercase bg-purple-100 text-purple-800 px-2 py-0.5 rounded-md font-semibold">
                      {h.type}
                    </span>
                  </div>
                  <span className="text-xs text-slate-400 flex items-center gap-1">
                    <Clock className="w-3 h-3" /> {h.createdAt ? new Date(h.createdAt).toLocaleString() : ""}
                  </span>
                </div>
                <p className="text-xs text-slate-600">{h.message}</p>
                {h.sentBy && <p className="text-[10px] text-slate-400">Sent by: {h.sentBy}</p>}
              </div>
            ))}

            {history.length === 0 && (
              <p className="text-center py-8 text-slate-400 text-sm">No notification history found.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
