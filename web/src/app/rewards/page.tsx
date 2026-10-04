"use client";

import { useEffect, useState } from "react";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Gift, Save, RefreshCw, CheckCircle2 } from "lucide-react";

export default function RewardsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  const [dailyRewardCoins, setDailyRewardCoins] = useState(50);
  const [referralBonusCoins, setReferralBonusCoins] = useState(100);
  const [coinsPerPoint, setCoinsPerPoint] = useState(10);
  const [lifeline5050Price, setLifeline5050Price] = useState(20);
  const [lifelineAudiencePrice, setLifelineAudiencePrice] = useState(20);
  const [lifelineSkipPrice, setLifelineSkipPrice] = useState(30);

  useEffect(() => {
    fetchRewardsConfig();
  }, []);

  const fetchRewardsConfig = async () => {
    setLoading(true);
    try {
      const snap = await getDoc(doc(db, "admin", "rewards"));
      if (snap.exists()) {
        const d = snap.data();
        setDailyRewardCoins(d.dailyRewardCoins ?? 50);
        setReferralBonusCoins(d.referralBonusCoins ?? 100);
        setCoinsPerPoint(d.coinsPerPoint ?? 10);
        setLifeline5050Price(d.lifeline5050Price ?? 20);
        setLifelineAudiencePrice(d.lifelineAudiencePrice ?? 20);
        setLifelineSkipPrice(d.lifelineSkipPrice ?? 30);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveRewards = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMsg("");

    try {
      await setDoc(doc(db, "admin", "rewards"), {
        dailyRewardCoins: Number(dailyRewardCoins),
        referralBonusCoins: Number(referralBonusCoins),
        coinsPerPoint: Number(coinsPerPoint),
        lifeline5050Price: Number(lifeline5050Price),
        lifelineAudiencePrice: Number(lifelineAudiencePrice),
        lifelineSkipPrice: Number(lifelineSkipPrice),
        updatedAt: new Date().toISOString(),
      }, { merge: true });

      setSuccessMsg("Rewards and lifeline config saved successfully!");
    } catch (e: any) {
      alert("Error: " + e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <div className="flex items-center gap-3">
        <div className="bg-amber-500 text-white p-3 rounded-2xl shadow-lg">
          <Gift className="w-8 h-8" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Rewards & Lifelines Config</h1>
          <p className="text-slate-500 text-sm mt-0.5">Manage daily reward coins, referral bonuses and lifeline costs</p>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-12 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" /> Loading rewards config...
        </div>
      ) : (
        <form onSubmit={handleSaveRewards} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-6">
          <div className="border-b pb-4">
            <h2 className="text-lg font-bold text-slate-900 mb-3">Rewards & Referrals</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Daily Check-in Coins</label>
                <input
                  type="number"
                  value={dailyRewardCoins}
                  onChange={(e) => setDailyRewardCoins(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Referral Bonus Coins</label>
                <input
                  type="number"
                  value={referralBonusCoins}
                  onChange={(e) => setReferralBonusCoins(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Coins Per Score Point</label>
                <input
                  type="number"
                  value={coinsPerPoint}
                  onChange={(e) => setCoinsPerPoint(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
            </div>
          </div>

          <div>
            <h2 className="text-lg font-bold text-slate-900 mb-3">Lifeline Coin Costs</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">50:50 Lifeline Price</label>
                <input
                  type="number"
                  value={lifeline5050Price}
                  onChange={(e) => setLifeline5050Price(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Audience Poll Price</label>
                <input
                  type="number"
                  value={lifelineAudiencePrice}
                  onChange={(e) => setLifelineAudiencePrice(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Skip Question Price</label>
                <input
                  type="number"
                  value={lifelineSkipPrice}
                  onChange={(e) => setLifelineSkipPrice(Number(e.target.value))}
                  className="w-full border rounded-xl px-4 py-2.5 text-sm"
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
            className="w-full bg-amber-600 hover:bg-amber-500 text-white font-medium py-3 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50 text-sm"
          >
            {saving ? "Saving Configuration..." : "Save Rewards Config"}
          </button>
        </form>
      )}
    </div>
  );
}
