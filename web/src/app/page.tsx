"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, query, limit } from "firebase/firestore";
import { db, auth } from "@/lib/firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useRouter } from "next/navigation";
import {
  FolderTree,
  HelpCircle,
  Users,
  CreditCard,
  BookOpen,
  ArrowUpRight,
  TrendingUp,
  Sparkles
} from "lucide-react";

export default function Dashboard() {
  const [stats, setStats] = useState({
    categories: 0,
    books: 0,
    questions: 0,
    users: 0,
    withdrawals: 0,
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
      const [catsSnap, booksSnap, qSnap, usersSnap, withdrawalsSnap] = await Promise.all([
        getDocs(collection(db, "categories")),
        getDocs(collection(db, "books")),
        getDocs(collection(db, "questions")),
        getDocs(collection(db, "users")),
        getDocs(collection(db, "withdrawals")),
      ]);

      setStats({
        categories: catsSnap.size,
        books: booksSnap.size,
        questions: qSnap.size,
        users: usersSnap.size,
        withdrawals: withdrawalsSnap.size,
      });
    } catch (error) {
      console.error("Error fetching stats:", error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    { title: "Total Categories", value: stats.categories, icon: FolderTree, color: "bg-blue-500", href: "/categories" },
    { title: "Total Books", value: stats.books, icon: BookOpen, color: "bg-indigo-500", href: "/categories" },
    { title: "Total Questions", value: stats.questions, icon: HelpCircle, color: "bg-emerald-500", href: "/questions" },
    { title: "Total Users", value: stats.users, icon: Users, color: "bg-amber-500", href: "/users" },
    { title: "Withdrawal Requests", value: stats.withdrawals, icon: CreditCard, color: "bg-rose-500", href: "/payments" },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-slate-500 text-sm mt-1">
            Welcome back! Here's an overview of QuizNova Platform.
          </p>
        </div>
        <div className="flex items-center gap-2 bg-indigo-50 border border-indigo-100 text-indigo-700 px-4 py-2 rounded-xl text-sm font-medium">
          <Sparkles className="w-4 h-4" />
          <span>Live Sync Active</span>
        </div>
      </div>

      {/* Stats Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              onClick={() => router.push(card.href)}
              className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm hover:shadow-md hover:border-slate-300 transition-all cursor-pointer group"
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

      {/* Quick Action Panels */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-gradient-to-br from-indigo-900 to-slate-900 text-white rounded-2xl p-6 shadow-xl relative overflow-hidden">
          <div className="relative z-10 space-y-4">
            <span className="bg-indigo-500/30 border border-indigo-400/30 text-indigo-300 text-xs font-semibold px-3 py-1 rounded-full uppercase tracking-wider">
              Gemini AI Powered
            </span>
            <h2 className="text-2xl font-bold">AI Question Generator</h2>
            <p className="text-indigo-200 text-sm max-w-md">
              Automatically generate multiple choice questions for any topic or book using Gemini AI API and batch save them to Firestore.
            </p>
            <button
              onClick={() => router.push("/ai-generator")}
              className="bg-white text-indigo-900 hover:bg-indigo-50 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-md inline-flex items-center gap-2"
            >
              <Sparkles className="w-4 h-4 text-indigo-600" />
              Generate Questions Now
            </button>
          </div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
          <h2 className="text-lg font-bold text-slate-900 mb-2">Quick Management Links</h2>
          <p className="text-slate-500 text-sm mb-4">Manage app data directly from web browser</p>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => router.push("/categories")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">Add Category / Book</p>
              <p className="text-xs text-slate-500">Structure quiz content</p>
            </button>
            <button
              onClick={() => router.push("/questions")}
              className="p-3 border border-slate-200 rounded-xl text-left hover:border-indigo-500 hover:bg-indigo-50/50 transition-all"
            >
              <p className="font-semibold text-slate-800 text-sm">Bulk Question Upload</p>
              <p className="text-xs text-slate-500">Upload JSON/CSV files</p>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
