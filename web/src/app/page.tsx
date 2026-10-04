"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, collectionGroup } from "firebase/firestore";
import { db, auth } from "@/lib/firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useRouter } from "next/navigation";
import {
  FolderTree,
  HelpCircle,
  Users,
  CreditCard,
  BookOpen,
  Bell,
  Sparkles,
  Trophy,
  ArrowUpRight,
  RefreshCw
} from "lucide-react";

export default function Dashboard() {
  const [stats, setStats] = useState({
    categories: 0,
    books: 0,
    questions: 0,
    users: 0,
    withdrawals: 0,
    notifications: 0,
    tournaments: 0,
  });
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (!user) {
        router.push("/login");
      } else {
        fetchDashboardData();
      }
    });
    return () => unsubscribe();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);

      // Fetch categories first
      const catsSnap = await getDocs(collection(db, "categories"));
      const catDocs = catsSnap.docs;
      const catNames = catDocs.map(d => d.data().name || d.id);

      // Fetch questions from all categories (questions/{catName}/items)
      let totalQ = 0;
      let totalB = 0;

      await Promise.all(catNames.map(async (cat) => {
        try {
          const qSnap = await getDocs(collection(db, "questions", cat, "items"));
          totalQ += qSnap.size;

          const bSnap = await getDocs(collection(db, "questions", cat, "books"));
          totalB += bSnap.size;
        } catch (e) {
          console.error(`Error loading items for cat ${cat}:`, e);
        }
      }));

      // Fallback: If totalQ is 0, try collectionGroup("items")
      if (totalQ === 0) {
        try {
          const groupSnap = await getDocs(collectionGroup(db, "items"));
          totalQ = groupSnap.size;
        } catch (e) {
          console.error("CollectionGroup query fallback failed:", e);
        }
      }

      const [usersSnap, withdrawalsSnap, notifSnap, tourneySnap] = await Promise.all([
        getDocs(collection(db, "users")).catch(() => ({ size: 0 })),
        getDocs(collection(db, "withdrawals")).catch(() => ({ size: 0 })),
        getDocs(collection(db, "notifications")).catch(() => ({ size: 0 })),
        getDocs(collection(db, "tournaments")).catch(() => ({ size: 0 })),
      ]);

      setStats({
        categories: catsSnap.size,
        books: totalB,
        questions: totalQ,
        users: usersSnap.size,
        withdrawals: withdrawalsSnap.size,
        notifications: notifSnap.size,
        tournaments: tourneySnap.size,
      });
    } catch (error) {
      console.error("Error fetching stats:", error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    { title: "Total Categories", value: stats.categories, icon: FolderTree, color: "bg-blue-500", href: "/categories" },
    { title: "Total Books", value: stats.books, icon: BookOpen, color: "bg-indigo-500", href: "/books" },
    { title: "Total Questions (MCQs)", value: stats.questions, icon: HelpCircle, color: "bg-emerald-500", href: "/questions" },
    { title: "Registered Users", value: stats.users, icon: Users, color: "bg-amber-500", href: "/users" },
    { title: "Withdrawal Requests", value: stats.withdrawals, icon: CreditCard, color: "bg-rose-500", href: "/payments" },
    { title: "Notifications Sent", value: stats.notifications, icon: Bell, color: "bg-purple-500", href: "/notifications" },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-slate-500 text-sm mt-1">
            Overview of QuizNova Platform, Quizzes, Users & Financials.
          </p>
        </div>
        <button
          onClick={fetchDashboardData}
          className="flex items-center gap-2 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 text-indigo-700 px-4 py-2 rounded-xl text-sm font-medium transition-all"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          <span>Refresh Stats</span>
        </button>
      </div>

      {/* Stats Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              onClick={() => router.push(card.href)}
              className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm hover:shadow-md hover:border-slate-300 transition-all cursor-pointer group"
            >
              <div className="flex items-center justify-between mb-4">
                <div className={`${card.color} text-white p-3 rounded-xl shadow-md`}>
                  <Icon className="w-6 h-6" />
                </div>
                <ArrowUpRight className="w-5 h-5 text-slate-400 group-hover:text-slate-600 transition-colors" />
              </div>
              <span className="text-slate-500 text-sm font-medium">{card.title}</span>
              <p className="text-3xl font-extrabold text-slate-900 mt-1">
                {loading ? "..." : card.value.toLocaleString()}
              </p>
            </div>
          );
        })}
      </div>

      {/* Quick Actions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-gradient-to-br from-indigo-900 to-slate-900 text-white rounded-2xl p-6 shadow-xl relative overflow-hidden">
          <div className="relative z-10 space-y-4">
            <span className="bg-indigo-500/30 border border-indigo-400/30 text-indigo-300 text-xs font-semibold px-3 py-1 rounded-full uppercase tracking-wider">
              AI Powered Feature
            </span>
            <h2 className="text-2xl font-bold">AI Question Generator</h2>
            <p className="text-indigo-200 text-sm max-w-md">
              Automatically generate MCQs for any category/topic using Gemini AI API and directly save them to Firestore!
            </p>
            <button
              onClick={() => router.push("/ai-generator")}
              className="bg-white text-indigo-900 hover:bg-indigo-50 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-md inline-flex items-center gap-2"
            >
              <Sparkles className="w-4 h-4 text-indigo-600" />
              Generate Questions
            </button>
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
          <h2 className="text-lg font-bold text-slate-900 mb-2">Quick Management Links</h2>
          <p className="text-slate-500 text-sm mb-4">Access all admin controls</p>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => router.push("/notifications")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">Send Notification</p>
              <p className="text-xs text-slate-500">Push to all or single user</p>
            </button>
            <button
              onClick={() => router.push("/questions")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">Bulk Questions</p>
              <p className="text-xs text-slate-500">JSON/CSV file import</p>
            </button>
            <button
              onClick={() => router.push("/tournaments")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">Tournaments</p>
              <p className="text-xs text-slate-500">Manage prize battles</p>
            </button>
            <button
              onClick={() => router.push("/settings")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">App Settings</p>
              <p className="text-xs text-slate-500">Ads, Version, Maintenance</p>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
