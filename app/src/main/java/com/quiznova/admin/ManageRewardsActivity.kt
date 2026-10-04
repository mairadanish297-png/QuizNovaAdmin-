package com.quiznova.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityManageRewardsBinding

class ManageRewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageRewardsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_manage_rewards)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        loadRewards()

        binding.btnSave.setOnClickListener { saveRewards() }
    }

    // ═══════════════════════════════════════════════
    //  LOAD FROM FIREBASE (matches RewardConfigManager)
    // ═══════════════════════════════════════════════

    private fun Any?.toLongVal(default: Long): Long {
        return when (this) {
            is Number -> this.toLong()
            is String -> this.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun loadRewards() {
        // Load main rewards document
        db.collection("admin").document("rewards").get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                // ── Quiz Rewards ──
                val quizRewards = doc.get("quizRewards") as? Map<*, *>

                (quizRewards?.get("easy") as? Map<*, *>)?.let { easy ->
                    binding.etEasyCoins.setText(easy["coinsPerCorrect"].toLongVal(5).toString())
                    binding.etEasyTime.setText(easy["time"].toLongVal(20).toString())
                    binding.etEasyNegative.setText(easy["negative"].toLongVal(0).toString())
                    binding.etEasyBonusThreshold.setText(easy["bonusThreshold"].toLongVal(7).toString())
                    binding.etEasyBonus.setText(easy["bonus"].toLongVal(10).toString())
                    binding.etEasyStreakBonus.setText(easy["streakBonus"].toLongVal(5).toString())
                    binding.etEasySkip.setText(easy["skipCount"].toLongVal(1).toString())
                    binding.switchEasy5050.isChecked = easy["lifeline5050"] as? Boolean ?: true
                }

                (quizRewards?.get("medium") as? Map<*, *>)?.let { med ->
                    binding.etMedCoins.setText(med["coinsPerCorrect"].toLongVal(10).toString())
                    binding.etMedTime.setText(med["time"].toLongVal(15).toString())
                    binding.etMedNegative.setText(med["negative"].toLongVal(2).toString())
                    binding.etMedBonusThreshold.setText(med["bonusThreshold"].toLongVal(7).toString())
                    binding.etMedBonus.setText(med["bonus"].toLongVal(25).toString())
                    binding.etMedStreakBonus.setText(med["streakBonus"].toLongVal(10).toString())
                    binding.etMedSkip.setText(med["skipCount"].toLongVal(1).toString())
                    binding.switchMed5050.isChecked = med["lifeline5050"] as? Boolean ?: true
                }

                (quizRewards?.get("hard") as? Map<*, *>)?.let { hard ->
                    binding.etHardCoins.setText(hard["coinsPerCorrect"].toLongVal(15).toString())
                    binding.etHardTime.setText(hard["time"].toLongVal(10).toString())
                    binding.etHardNegative.setText(hard["negative"].toLongVal(5).toString())
                    binding.etHardBonusThreshold.setText(hard["bonusThreshold"].toLongVal(8).toString())
                    binding.etHardBonus.setText(hard["bonus"].toLongVal(50).toString())
                    binding.etHardStreakBonus.setText(hard["streakBonus"].toLongVal(15).toString())
                    binding.etHardSkip.setText(hard["skipCount"].toLongVal(0).toString())
                    binding.switchHard5050.isChecked = hard["lifeline5050"] as? Boolean ?: true
                }

                // ── Daily Rewards ──
                val daily = doc.get("dailyRewards") as? Map<*, *>
                if (daily != null) {
                    val dayFields = listOf(
                        binding.etDay1, binding.etDay2, binding.etDay3,
                        binding.etDay4, binding.etDay5, binding.etDay6, binding.etDay7
                    )
                    for (i in 1..7) {
                        dayFields[i - 1].setText(daily["day$i"].toLongVal((5 * i).toLong()).toString())
                    }
                }

                // ── Streak Rewards ──
                val streak = doc.get("streakRewards") as? Map<*, *>
                if (streak != null) {
                    binding.etStreak3.setText(streak["streak3"].toLongVal(10).toString())
                    binding.etStreak7.setText(streak["streak7"].toLongVal(25).toString())
                    binding.etStreak14.setText(streak["streak14"].toLongVal(50).toString())
                    binding.etStreak30.setText(streak["streak30"].toLongVal(100).toString())
                }

                // ── Spin Wheel ──
                val spinWheel = doc.get("spinWheel") as? Map<*, *>
                if (spinWheel != null) {
                    binding.etDailySpins.setText(spinWheel["dailySpins"].toLongVal(1).toString())

                    val segments = spinWheel["segments"] as? List<*>
                    if (segments != null) {
                        val sb = StringBuilder()
                        for (seg in segments) {
                            if (seg is Map<*, *>) {
                                val coins = seg["coins"].toLongVal(0)
                                val weight = seg["weight"].toLongVal(10)
                                sb.appendLine("$coins,$weight")
                            }
                        }
                        binding.etSegments.setText(sb.toString().trim())
                    }
                }

                // ── Referral & Premium ──
                val referral = doc.get("referralReward").toLongVal(50L)
                binding.etReferralReward.setText(referral.toString())
                
                val premPrice = doc.get("premiumPriceCoins").toLongVal(500L)
                binding.etPremiumPrice.setText(premPrice.toString())

                // ── Withdrawal ──
                val withdrawal = doc.get("withdrawal") as? Map<*, *>
                if (withdrawal != null) {
                    binding.etMinWithdrawal.setText(withdrawal["minAmount"].toLongVal(100).toString())
                    binding.etMaxWithdrawal.setText(withdrawal["maxAmount"].toLongVal(5000).toString())
                }
            }

        // Load coin rate from config
        db.collection("admin").document("config").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val rate = doc.get("coinToDollarRate").toLongVal(100L)
                    binding.etCoinRate.setText(rate.toString())
                }
            }
    }

    // ═══════════════════════════════════════════════
    //  SAVE TO FIREBASE (matches RewardConfigManager)
    // ═══════════════════════════════════════════════

    private fun saveRewards() {
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "SAVING..."

        val segmentsList = parseSegments()

        val rewardsData = hashMapOf<String, Any>(
            "quizRewards" to hashMapOf(
                "easy" to hashMapOf(
                    "coinsPerCorrect" to intOf(binding.etEasyCoins, 5),
                    "time" to intOf(binding.etEasyTime, 20),
                    "negative" to intOf(binding.etEasyNegative, 0),
                    "bonusThreshold" to intOf(binding.etEasyBonusThreshold, 7),
                    "bonus" to intOf(binding.etEasyBonus, 10),
                    "streakBonus" to intOf(binding.etEasyStreakBonus, 5),
                    "skipCount" to intOf(binding.etEasySkip, 1),
                    "lifeline5050" to binding.switchEasy5050.isChecked
                ),
                "medium" to hashMapOf(
                    "coinsPerCorrect" to intOf(binding.etMedCoins, 10),
                    "time" to intOf(binding.etMedTime, 15),
                    "negative" to intOf(binding.etMedNegative, 2),
                    "bonusThreshold" to intOf(binding.etMedBonusThreshold, 7),
                    "bonus" to intOf(binding.etMedBonus, 25),
                    "streakBonus" to intOf(binding.etMedStreakBonus, 10),
                    "skipCount" to intOf(binding.etMedSkip, 1),
                    "lifeline5050" to binding.switchMed5050.isChecked
                ),
                "hard" to hashMapOf(
                    "coinsPerCorrect" to intOf(binding.etHardCoins, 15),
                    "time" to intOf(binding.etHardTime, 10),
                    "negative" to intOf(binding.etHardNegative, 5),
                    "bonusThreshold" to intOf(binding.etHardBonusThreshold, 8),
                    "bonus" to intOf(binding.etHardBonus, 50),
                    "streakBonus" to intOf(binding.etHardStreakBonus, 15),
                    "skipCount" to intOf(binding.etHardSkip, 0),
                    "lifeline5050" to binding.switchHard5050.isChecked
                )
            ),
            "dailyRewards" to hashMapOf(
                "day1" to intOf(binding.etDay1, 5),
                "day2" to intOf(binding.etDay2, 10),
                "day3" to intOf(binding.etDay3, 15),
                "day4" to intOf(binding.etDay4, 20),
                "day5" to intOf(binding.etDay5, 25),
                "day6" to intOf(binding.etDay6, 30),
                "day7" to intOf(binding.etDay7, 35)
            ),
            "streakRewards" to hashMapOf(
                "streak3" to intOf(binding.etStreak3, 10),
                "streak7" to intOf(binding.etStreak7, 25),
                "streak14" to intOf(binding.etStreak14, 50),
                "streak30" to intOf(binding.etStreak30, 100)
            ),
            "spinWheel" to hashMapOf<String, Any>(
                "dailySpins" to intOf(binding.etDailySpins, 1),
                "segments" to segmentsList
            ),
            "referralReward" to intOf(binding.etReferralReward, 50),
            "premiumPriceCoins" to intOf(binding.etPremiumPrice, 500),
            "withdrawal" to hashMapOf(
                "minAmount" to intOf(binding.etMinWithdrawal, 100),
                "maxAmount" to intOf(binding.etMaxWithdrawal, 5000)
            )
        )

        // Save to admin/rewards
        db.collection("admin").document("rewards")
            .set(rewardsData)
            .addOnSuccessListener {
                // Save coin rate to admin/config
                val configData = mapOf(
                    "coinToDollarRate" to intOf(binding.etCoinRate, 100)
                )
                db.collection("admin").document("config")
                    .update(configData)
                    .addOnSuccessListener {
                        onSaveSuccess()
                    }
                    .addOnFailureListener {
                        // Config update failed, but rewards saved OK
                        db.collection("admin").document("config").set(configData)
                            .addOnSuccessListener { onSaveSuccess() }
                            .addOnFailureListener { e -> onSaveFailed(e.message) }
                    }
            }
            .addOnFailureListener { e ->
                onSaveFailed(e.message)
            }
    }

    private fun onSaveSuccess() {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "SAVE ALL SETTINGS"
        binding.tvSaveStatus.text = "All settings saved successfully!"
        binding.tvSaveStatus.visibility = View.VISIBLE
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        logAction("rewards_config_update")
    }

    private fun onSaveFailed(message: String?) {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "SAVE ALL SETTINGS"
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    // ═══════════════════════════════════════════════
    //  PARSE SEGMENTS
    // ═══════════════════════════════════════════════

    private fun parseSegments(): List<HashMap<String, Int>> {
        val text = binding.etSegments.text.toString().trim()
        if (text.isEmpty()) return getDefaultSegments()

        val segments = mutableListOf<HashMap<String, Int>>()
        val lines = text.split("\n")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val parts = trimmed.split(",")
            if (parts.size == 2) {
                val coins = parts[0].trim().toIntOrNull() ?: 0
                val weight = parts[1].trim().toIntOrNull() ?: 10
                segments.add(hashMapOf("coins" to coins, "weight" to weight))
            }
        }

        return if (segments.isEmpty()) getDefaultSegments() else segments
    }

    private fun getDefaultSegments(): List<HashMap<String, Int>> {
        return listOf(
            hashMapOf("coins" to 0, "weight" to 25),
            hashMapOf("coins" to 5, "weight" to 8),
            hashMapOf("coins" to 10, "weight" to 15),
            hashMapOf("coins" to 15, "weight" to 20),
            hashMapOf("coins" to 50, "weight" to 3),
            hashMapOf("coins" to 20, "weight" to 18),
            hashMapOf("coins" to 30, "weight" to 10),
            hashMapOf("coins" to 100, "weight" to 5)
        )
    }

    // ═══════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════

    private fun intOf(editText: android.widget.EditText, default: Int): Int {
        return editText.text.toString().toIntOrNull() ?: default
    }

    private fun logAction(details: String) {
        val log = hashMapOf(
            "action" to "rewards_update",
            "details" to details,
            "adminEmail" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("admin").document("logs").collection("entries").add(log)
    }
}