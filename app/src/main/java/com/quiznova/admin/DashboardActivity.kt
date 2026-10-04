package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiznova.admin.databinding.ActivityDashboardBinding
import com.quiznova.admin.firebase.CategoryHelper

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupClicks()
        loadStats()
        loadRecentActivity()
        loadPendingPaymentsCount()
    }

    private fun setupHeader() {
        binding.includeHeader.tvTitle.text = getString(R.string.title_dashboard)
        binding.includeHeader.btnBack.isVisible = false

        val email = FirebaseAuth.getInstance().currentUser?.email ?: getString(R.string.label_admin_default)
        binding.tvAdminEmail.text = email
    }

    private fun setupClicks() {
        binding.actionGenerate.setOnClickListener { navigateTo(GenerateActivity::class.java, true) }
        binding.actionManageQuestions.setOnClickListener { navigateTo(ManageQuestionsActivity::class.java, true) }
        binding.actionUsers.setOnClickListener { navigateTo(ManageUsersActivity::class.java) }
        binding.actionCategories.setOnClickListener { navigateTo(ManageCategoriesActivity::class.java, true) }
        binding.actionRewards.setOnClickListener { navigateTo(ManageRewardsActivity::class.java) }
        binding.actionWithdrawals.setOnClickListener { navigateTo(ManageWithdrawalsActivity::class.java) }
        binding.actionAnalytics.setOnClickListener { navigateTo(AnalyticsActivity::class.java, true) }
        binding.actionNotifications.setOnClickListener { navigateTo(NotificationsActivity::class.java) }
        binding.actionSettings.setOnClickListener { navigateTo(AdminSettingsActivity::class.java) }
        binding.actionBulkUpload.setOnClickListener { navigateTo(BulkUploadActivity::class.java, true) }
        binding.actionPaymentRequests.setOnClickListener { navigateTo(PaymentRequestsActivity::class.java) }
        binding.actionBattles.setOnClickListener { 
            navigateTo(ManageBattlesActivity::class.java)
        }
        binding.actionTournaments.setOnClickListener {
            navigateTo(ManageTournamentsActivity::class.java)
        }
        binding.actionPlans.setOnClickListener {
            navigateTo(ManagePlansActivity::class.java)
        }

        binding.statUsers.setOnClickListener { navigateTo(ManageUsersActivity::class.java) }
        binding.statQuestions.setOnClickListener { navigateTo(ManageQuestionsActivity::class.java, true) }
        binding.statCategories.setOnClickListener { navigateTo(ManageCategoriesActivity::class.java, true) }
        binding.statWithdrawals.setOnClickListener { navigateTo(ManageWithdrawalsActivity::class.java) }
        binding.statPremium.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("filter", "premium")
            startActivity(intent)
        }
    }

    private fun <T> navigateTo(activityClass: Class<T>, clearCache: Boolean = false) {
        if (clearCache) CategoryHelper.clearCache()
        startActivity(Intent(this, activityClass))
    }

    private fun loadStats() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                binding.tvStatUsers.text = snapshot.size().toString()

                var premium = 0
                var totalQuizzes = 0
                for (doc in snapshot.documents) {
                    if (doc.getBoolean("isPremium") == true) premium++
                    totalQuizzes += doc.getLong("quizzesPlayed")?.toInt() ?: 0
                }
                binding.tvStatPremium.text = premium.toString()
                binding.tvStatQuizzes.text = totalQuizzes.toString()
            }

        CategoryHelper.getCategoryNames { categories ->
            if (categories.isEmpty()) {
                runOnUiThread { binding.tvStatQuestions.text = getString(R.string.default_zero) }
                return@getCategoryNames
            }

            var totalQuestions = 0
            var categoriesLoaded = 0
            val totalCategories = categories.size

            for (cat in categories) {
                db.collection("questions")
                    .document(cat)
                    .collection("items")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        totalQuestions += snapshot.size()
                        categoriesLoaded++
                        if (categoriesLoaded >= totalCategories) {
                            runOnUiThread { binding.tvStatQuestions.text = totalQuestions.toString() }
                        }
                    }
                    .addOnFailureListener {
                        categoriesLoaded++
                        if (categoriesLoaded >= totalCategories) {
                            runOnUiThread { binding.tvStatQuestions.text = totalQuestions.toString() }
                        }
                    }
            }
        }

        db.collection("categories").get()
            .addOnSuccessListener { snapshot ->
                binding.tvStatCategories.text = snapshot.size().toString()
            }

        db.collection("withdrawals")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvStatWithdrawals.text = snapshot.size().toString()
            }
    }

    private fun loadPendingPaymentsCount() {
        db.collection("payment_requests")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                if (count > 0) {
                    binding.tvPendingPayments.text = "$count pending"
                    binding.tvPendingPayments.setTextColor(
                        ContextCompat.getColor(this, R.color.status_warning)
                    )
                } else {
                    binding.tvPendingPayments.text = "No pending"
                    binding.tvPendingPayments.setTextColor(
                        ContextCompat.getColor(this, R.color.text_muted)
                    )
                }
            }
    }

    private fun loadRecentActivity() {
        db.collection("admin")
            .document("logs")
            .collection("entries")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvNoActivity.isVisible = snapshot.isEmpty
                binding.layoutActivityLog.removeAllViews()

                if (snapshot.isEmpty) {
                    binding.layoutActivityLog.addView(binding.tvNoActivity)
                    return@addOnSuccessListener
                }

                val textColor = ContextCompat.getColor(this, R.color.text_secondary)

                for (doc in snapshot.documents) {
                    val details = doc.getString("details") ?: ""
                    val tsVal = doc.get("timestamp")
                    val time = when (tsVal) {
                        is com.google.firebase.Timestamp -> tsVal.toDate().toString().take(16)
                        is Number -> java.util.Date(tsVal.toLong()).toString().take(16)
                        else -> ""
                    }

                    val logView = TextView(this).apply {
                        text = getString(R.string.format_log_entry, details, time)
                        setTextColor(textColor)
                        textSize = 13f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.layoutActivityLog.addView(logView)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        loadRecentActivity()
        loadPendingPaymentsCount()
    }
}