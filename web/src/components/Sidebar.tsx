"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import {
  LayoutDashboard,
  FolderTree,
  BookOpen,
  HelpCircle,
  Bot,
  Bell,
  Sparkles,
  Users,
  CreditCard,
  Gift,
  Trophy,
  Sliders,
  Settings,
  LogOut
} from "lucide-react";

const navItems = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard },
  { name: "Categories", href: "/categories", icon: FolderTree },
  { name: "Books & Chapters", href: "/books", icon: BookOpen },
  { name: "Questions Manager", href: "/questions", icon: HelpCircle },
  { name: "AI Question Generator", href: "/ai-generator", icon: Bot },
  { name: "Push Notifications", href: "/notifications", icon: Bell },
  { name: "Subscription Plans", href: "/plans", icon: Sparkles },
  { name: "Tournaments & Battles", href: "/tournaments", icon: Trophy },
  { name: "Users & Lifelines", href: "/users", icon: Users },
  { name: "Payments & Withdrawals", href: "/payments", icon: CreditCard },
  { name: "Rewards Config", href: "/rewards", icon: Gift },
  { name: "Admin Settings", href: "/settings", icon: Settings },
];

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();

  if (pathname === "/login") return null;

  const handleLogout = async () => {
    await signOut(auth);
    router.push("/login");
  };

  return (
    <aside className="w-64 bg-slate-900 text-slate-200 h-screen flex flex-col justify-between p-4 shadow-xl fixed left-0 top-0 z-50 overflow-y-auto">
      <div>
        <div className="flex items-center gap-3 px-3 py-4 mb-4 border-b border-slate-800">
          <div className="bg-indigo-600 p-2 rounded-xl text-white shadow-lg shadow-indigo-500/30">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h1 className="font-bold text-lg text-white leading-none">QuizNova</h1>
            <span className="text-xs text-indigo-400 font-medium">Full Web Admin</span>
          </div>
        </div>

        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? "bg-indigo-600 text-white shadow-md shadow-indigo-600/30"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/60"
                }`}
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      <div className="border-t border-slate-800 pt-4 mt-6">
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-rose-400 hover:bg-rose-950/40 hover:text-rose-300 transition-all"
        >
          <LogOut className="w-4 h-4" />
          Sign Out
        </button>
      </div>
    </aside>
  );
}
