package com.quiznova.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityUserDetailsBinding

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var userId = ""
    private var isBanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_user_details)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_id_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserDetails()
        setupActions()
    }

    private fun loadUserDetails() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, getString(R.string.msg_user_not_found), Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                binding.tvUsername.text = doc.getString("username") ?: "Unknown"
                binding.tvEmail.text = doc.getString("email") ?: ""

                val status = doc.getString("status") ?: "active"
                isBanned = status == "banned"

                if (isBanned) {
                    binding.tvStatus.setTextColor(getColor(R.color.status_error))
                    binding.tvStatus.text = getString(R.string.status_banned)
                    binding.btnBan.visibility = View.GONE
                    binding.btnUnban.visibility = View.VISIBLE
                } else {
                    binding.tvStatus.setTextColor(getColor(R.color.status_success))
                    binding.tvStatus.text = getString(R.string.status_active)
                    binding.btnBan.visibility = View.VISIBLE
                    binding.btnUnban.visibility = View.GONE
                }

                val isPremium = doc.getBoolean("isPremium") ?: false
                if (isPremium) {
                    binding.tvPremium.visibility = View.VISIBLE
                    binding.btnMakePremium.text = getString(R.string.label_already_premium)
                    binding.btnMakePremium.isEnabled = false
                } else {
                    binding.tvPremium.visibility = View.GONE
                    binding.btnMakePremium.text = getString(R.string.btn_make_premium)
                    binding.btnMakePremium.isEnabled = true
                }

                binding.tvTotalScore.text = (doc.getLong("totalScore")?.toInt() ?: 0).toString()
                binding.tvTotalCoins.text = (doc.getLong("totalCoins")?.toInt() ?: 0).toString()
                
                val wallet = doc.get("walletBalance")
                val walletDouble = when(wallet) {
                    is Number -> wallet.toDouble()
                    else -> 0.0
                }
                binding.tvWalletBalance.text = String.format("Rs. %.2f", walletDouble)

                binding.tvQuizzesPlayed.text = (doc.getLong("quizzesPlayed")?.toInt() ?: 0).toString()
                binding.tvCorrectAnswers.text = (doc.getLong("correctAnswers")?.toInt() ?: 0).toString()
                binding.tvBestStreak.text = (doc.getLong("bestStreak")?.toInt() ?: 0).toString()

                val totalAnswers = doc.getLong("totalAnswers")?.toInt() ?: 0
                val correctAnswers = doc.getLong("correctAnswers")?.toInt() ?: 0
                val accuracy = if (totalAnswers > 0) (correctAnswers * 100 / totalAnswers) else 0
                binding.tvAccuracy.text = getString(R.string.format_accuracy, accuracy)

                binding.tvJoined.text = doc.get("joinedDate")?.toString() ?: "N/A"
                binding.tvLastActive.text = doc.get("lastActiveDate")?.toString() ?: "N/A"
                binding.tvReferralCode.text = doc.getString("referralCode") ?: "N/A"
                binding.tvReferralCount.text = (doc.getLong("referralCount")?.toInt() ?: 0).toString()
            }
    }

    private fun setupActions() {
        // Give Coins (supports negative for removal)
        binding.btnGiveCoins.setOnClickListener {
            val amount = binding.etGiveCoins.text.toString().trim().toIntOrNull()
            if (amount == null) {
                Toast.makeText(this, getString(R.string.msg_enter_valid_amount), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId)
                .update("totalCoins", com.google.firebase.firestore.FieldValue.increment(amount.toLong()))
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Coins adjusted by $amount", Toast.LENGTH_SHORT).show()
                    binding.etGiveCoins.text.clear()
                    loadUserDetails()
                    logAction("Adjusted coins by $amount for user $userId")
                }
        }

        // Set Score
        binding.btnAdjustScore.setOnClickListener {
            val amount = binding.etAdjustScore.text.toString().trim().toIntOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter valid score", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId)
                .update(mapOf("totalScore" to amount.toLong()))
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Score updated!", Toast.LENGTH_SHORT).show()
                    binding.etAdjustScore.text.clear()
                    loadUserDetails()
                    logAction("Set score to $amount for user $userId")
                }
        }

        // Set Wallet
        binding.btnAdjustWallet.setOnClickListener {
            val amount = binding.etAdjustWallet.text.toString().trim().toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter valid wallet amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId)
                .update(mapOf("walletBalance" to amount))
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Wallet updated!", Toast.LENGTH_SHORT).show()
                    binding.etAdjustWallet.text.clear()
                    loadUserDetails()
                    logAction("Set wallet to Rs. $amount for user $userId")
                }
        }

        // Gift Lifelines
        binding.btnGiftLifelines.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Gift Lifelines?")
                .setMessage("Add 5 of each lifeline (50-50, Skip, Poll) to this user?")
                .setPositiveButton("Gift") { _, _ ->
                    db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                        val current = doc.get("lifelines") as? Map<*, *> ?: emptyMap<String, Any>()
                        val newLifelines = hashMapOf(
                            "fiftyFifty" to ((current["fiftyFifty"] as? Long ?: 0L) + 5L),
                            "skip" to ((current["skip"] as? Long ?: 0L) + 5L),
                            "poll" to ((current["poll"] as? Long ?: 0L) + 5L)
                        )
                        db.collection("users").document(userId).update("lifelines", newLifelines)
                            .addOnSuccessListener {
                                Toast.makeText(this, "✅ Lifelines gifted!", Toast.LENGTH_SHORT).show()
                                logAction("Gifted lifelines to user $userId")
                            }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Make Premium
        binding.btnMakePremium.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Make Premium?")
                .setMessage("Grant premium access to this user for 30 days?")
                .setPositiveButton("Yes") { _, _ ->
                    val expiry = com.google.firebase.Timestamp(
                        java.util.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                    )
                    db.collection("users").document(userId)
                        .update(
                            "isPremium", true,
                            "premiumExpiry", expiry
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.msg_premium_granted), Toast.LENGTH_SHORT).show()
                            loadUserDetails()
                            logAction("Made user $userId premium")
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Ban
        binding.btnBan.setOnClickListener {
            val input = android.widget.EditText(this).apply {
                hint = "Ban reason"
                setPadding(40, 30, 40, 30)
            }

            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Ban User?")
                .setMessage("This user will not be able to use the app.")
                .setView(input)
                .setPositiveButton("Ban") { _, _ ->
                    val reason = input.text.toString().trim().ifEmpty { "No reason" }
                    db.collection("users").document(userId)
                        .update(
                            "status", "banned",
                            "banReason", reason
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.msg_user_banned), Toast.LENGTH_SHORT).show()
                            loadUserDetails()
                            logAction("Banned user $userId: $reason")
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Unban
        binding.btnUnban.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Unban User?")
                .setPositiveButton("Unban") { _, _ ->
                    db.collection("users").document(userId)
                        .update("status", "active", "banReason", "")
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.msg_user_unbanned), Toast.LENGTH_SHORT).show()
                            loadUserDetails()
                            logAction("Unbanned user $userId")
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Delete User Data
        binding.btnDeleteUser.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Delete User Data?")
                .setMessage("This will permanently delete all user data. This cannot be undone!")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("users").document(userId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.msg_user_deleted), Toast.LENGTH_SHORT).show()
                            logAction("Deleted user $userId")
                            finish()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun logAction(details: String) {
        val log = hashMapOf(
            "action" to "user_management",
            "details" to details,
            "adminEmail" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("admin").document("logs").collection("entries").add(log)
    }
}