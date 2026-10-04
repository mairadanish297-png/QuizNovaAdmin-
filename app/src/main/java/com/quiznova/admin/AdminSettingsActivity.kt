package com.quiznova.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityAdminSettingsBinding

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSettingsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Admin Settings"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        loadConfig()
        setupSave()
        setupGlobalSave()
        setupFinancialsSave()
        setupSupportSave()
        setupAdsSave()
    }

    private fun loadConfig() {
        db.collection("admin").document("config").get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val provider = doc.getString("aiProvider") ?: "groq"
                val key = doc.getString("aiApiKey") ?: ""

                when (provider) {
                    "groq" -> binding.radioGroq.isChecked = true
                    "gemini" -> binding.radioGemini.isChecked = true
                }

                binding.etApiKey.setText(key)

                // Global Controls
                val maintenance = doc.getBoolean("maintenanceMode") ?: false
                val battle = doc.getBoolean("enableBattleMode") ?: true
                val tournament = doc.getBoolean("enableTournamentMode") ?: true
                val adsEnabled = doc.getBoolean("adsEnabled") ?: true
                val target = doc.getLong("targetUsers") ?: 1000L
                val minVersion = doc.getLong("minVersion") ?: 1L
                val totalUsers = doc.getLong("totalUsers") ?: 0L
                val override = doc.getBoolean("manualOverride") ?: false

                binding.switchMaintenance.isChecked = maintenance
                binding.switchBattle.isChecked = battle
                binding.switchTournament.isChecked = tournament
                binding.switchAds.isChecked = adsEnabled
                binding.etTargetUsers.setText(target.toString())
                binding.etMinVersion.setText(minVersion.toString())
                binding.tvDisplayTotalUsers.text = totalUsers.toString()
                binding.switchOverride.isChecked = override

                // Financials
                val pkrRate = doc.getLong("pkrPer1000") ?: 10L
                val minWithdraw = doc.getLong("minWithdrawCoins") ?: 10000L
                binding.etPkrRate.setText(pkrRate.toString())
                binding.etMinWithdrawAmount.setText(minWithdraw.toString())

                // Support
                binding.etSupportNumber.setText(doc.getString("supportNumber") ?: "")

                // Ads Units
                binding.etBannerId.setText(doc.getString("admobBannerId") ?: "")
                binding.etInterstitialId.setText(doc.getString("admobInterstitialId") ?: "")
                binding.etRewardedId.setText(doc.getString("admobRewardedId") ?: "")
                binding.etNativeId.setText(doc.getString("admobNativeId") ?: "")

                if (key.isNotEmpty()) {
                    binding.tvKeyStatus.text = "✅ Key set (${key.take(8)}...)"
                    binding.tvKeyStatus.setTextColor(getColor(R.color.status_success))
                } else {
                    binding.tvKeyStatus.text = "❌ No key set"
                    binding.tvKeyStatus.setTextColor(getColor(R.color.status_error))
                }
            }
    }

    private fun setupSave() {
        binding.btnSaveAi.setOnClickListener {
            val provider = if (binding.radioGroq.isChecked) "groq" else "gemini"
            val key = binding.etApiKey.text.toString().trim()

            if (key.isEmpty()) {
                Toast.makeText(this, "Enter API key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf<String, Any>(
                "aiProvider" to provider,
                "aiApiKey" to key
            )

            db.collection("admin").document("config")
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ API key saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupGlobalSave() {
        binding.btnSaveGlobal.setOnClickListener {
            val maintenance = binding.switchMaintenance.isChecked
            val battle = binding.switchBattle.isChecked
            val tournament = binding.switchTournament.isChecked
            val ads = binding.switchAds.isChecked
            val target = binding.etTargetUsers.text.toString().toLongOrNull() ?: 1000L
            val minVersion = binding.etMinVersion.text.toString().toLongOrNull() ?: 1L
            val override = binding.switchOverride.isChecked

            val data = hashMapOf<String, Any>(
                "maintenanceMode" to maintenance,
                "enableBattleMode" to battle,
                "enableTournamentMode" to tournament,
                "adsEnabled" to ads,
                "targetUsers" to target,
                "minVersion" to minVersion,
                "manualOverride" to override
            )

            db.collection("admin").document("config")
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_global_saved), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupFinancialsSave() {
        binding.btnSaveFinancials.setOnClickListener {
            val pkrRate = binding.etPkrRate.text.toString().toLongOrNull() ?: 10L
            val minWithdraw = binding.etMinWithdrawAmount.text.toString().toLongOrNull() ?: 10000L

            val data = hashMapOf<String, Any>(
                "pkrPer1000" to pkrRate,
                "minWithdrawCoins" to minWithdraw
            )

            db.collection("admin").document("config")
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Financials saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupSupportSave() {
        binding.btnSaveSupport.setOnClickListener {
            val number = binding.etSupportNumber.text.toString().trim()

            db.collection("admin").document("config")
                .update("supportNumber", number)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Support number saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupAdsSave() {
        binding.btnSaveAds.setOnClickListener {
            val banner = binding.etBannerId.text.toString().trim()
            val interstitial = binding.etInterstitialId.text.toString().trim()
            val rewarded = binding.etRewardedId.text.toString().trim()
            val native = binding.etNativeId.text.toString().trim()

            val data = hashMapOf<String, Any>(
                "admobBannerId" to banner,
                "admobInterstitialId" to interstitial,
                "admobRewardedId" to rewarded,
                "admobNativeId" to native
            )

            db.collection("admin").document("config")
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Ad Unit IDs saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
