"use client";

import { useEffect, useState } from "react";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Settings, Save, RefreshCw, CheckCircle2, ShieldAlert } from "lucide-react";

export default function SettingsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  const [maintenance, setMaintenance] = useState(false);
  const [forceUpdate, setForceUpdate] = useState(false);
  const [minVersion, setMinVersion] = useState(1);
  const [minWithdrawal, setMinWithdrawal] = useState(100);
  const [currencySymbol, setCurrencySymbol] = useState("Rs");

  // AdMob Ad Units
  const [bannerAdId, setBannerAdId] = useState("");
  const [interstitialAdId, setInterstitialAdId] = useState("");
  const [rewardedAdId, setRewardedAdId] = useState("");

  useEffect(() => {
    fetchConfig();
  }, []);

  const fetchConfig = async () => {
    setLoading(true);
    try {
      const snap = await getDoc(doc(db, "admin", "config"));
      if (snap.exists()) {
        const d = snap.data();
        setMaintenance(d.maintenance ?? false);
        setForceUpdate(d.forceUpdate ?? false);
        setMinVersion(d.minVersion ?? 1);
        setMinWithdrawal(d.minWithdrawal ?? 100);
        setCurrencySymbol(d.currencySymbol || "Rs");
        setBannerAdId(d.bannerAdId || "");
        setInterstitialAdId(d.interstitialAdId || "");
        setRewardedAdId(d.rewardedAdId || "");
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMsg("");

    try {
      await setDoc(doc(db, "admin", "config"), {
        maintenance,
        forceUpdate,
        minVersion: Number(minVersion),
        minWithdrawal: Number(minWithdrawal),
        currencySymbol,
        bannerAdId,
        interstitialAdId,
        rewardedAdId,
        updatedAt: new Date().toISOString(),
      }, { merge: true });

      setSuccessMsg("App config successfully saved to Firestore!");
    } catch (e: any) {
      alert("Error: " + e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <div className="flex items-center gap-3">
        <div className="bg-slate-900 text-white p-3 rounded-2xl shadow-lg">
          <Settings className="w-8 h-8" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Admin & App Settings</h1>
          <p className="text-slate-500 text-sm mt-0.5">Control app versioning, maintenance mode and AdMob ad units</p>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading settings...
        </div>
      ) : (
        <form onSubmit={handleSaveConfig} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
          <div className="border-b pb-4">
            <h2 className="text-lg font-bold text-slate-900 mb-3">App Control & Status</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="flex items-center gap-3 p-4 border rounded-xl cursor-pointer hover:bg-slate-50">
                <input
                  type="checkbox"
                  checked={maintenance}
                  onChange={(e) => setMaintenance(e.target.checked)}
                  className="w-5 h-5 text-indigo-600 rounded focus:ring-indigo-500"
                />
                <div>
                  <p className="font-semibold text-slate-800 text-sm">Maintenance Mode</p>
                  <p className="text-xs text-slate-400">Block app access for server updates</p>
                </div>
              </label>

              <label className="flex items-center gap-3 p-4 border rounded-xl cursor-pointer hover:bg-slate-50">
                <input
                  type="checkbox"
                  checked={forceUpdate}
                  onChange={(e) => setForceUpdate(e.target.checked)}
                  className="w-5 h-5 text-indigo-600 rounded focus:ring-indigo-500"
                />
                <div>
                  <p className="font-semibold text-slate-800 text-sm">Force App Update</p>
                  <p className="text-xs text-slate-400">Require users to update to latest version</p>
                </div>
              </label>
            </div>
          </div>

          <div className="border-b pb-4 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Min Version Code</label>
              <input
                type="number"
                value={minVersion}
                onChange={(e) => setMinVersion(Number(e.target.value))}
                className="w-full border rounded-xl px-4 py-2.5 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Min Withdrawal (PKR)</label>
              <input
                type="number"
                value={minWithdrawal}
                onChange={(e) => setMinWithdrawal(Number(e.target.value))}
                className="w-full border rounded-xl px-4 py-2.5 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Currency Symbol</label>
              <input
                type="text"
                value={currencySymbol}
                onChange={(e) => setCurrencySymbol(e.target.value)}
                className="w-full border rounded-xl px-4 py-2.5 text-sm"
              />
            </div>
          </div>

          <div>
            <h2 className="text-lg font-bold text-slate-900 mb-3">Google AdMob Ad Units</h2>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Banner Ad Unit ID</label>
                <input
                  type="text"
                  value={bannerAdId}
                  onChange={(e) => setBannerAdId(e.target.value)}
                  placeholder="ca-app-pub-3940256099942544/6300978111"
                  className="w-full border rounded-xl px-4 py-2 text-sm font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Interstitial Ad Unit ID</label>
                <input
                  type="text"
                  value={interstitialAdId}
                  onChange={(e) => setInterstitialAdId(e.target.value)}
                  placeholder="ca-app-pub-3940256099942544/1033173712"
                  className="w-full border rounded-xl px-4 py-2 text-sm font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Rewarded Ad Unit ID</label>
                <input
                  type="text"
                  value={rewardedAdId}
                  onChange={(e) => setRewardedAdId(e.target.value)}
                  placeholder="ca-app-pub-3940256099942544/5224354917"
                  className="w-full border rounded-xl px-4 py-2 text-sm font-mono"
                />
              </div>
            </div>
          </div>

          {successMsg && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-xl flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={saving}
            className="w-full bg-slate-900 hover:bg-slate-800 text-white font-medium py-3 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50 text-sm"
          >
            {saving ? "Saving Configuration..." : "Save App Configuration"}
          </button>
        </form>
      )}
    </div>
  );
}
