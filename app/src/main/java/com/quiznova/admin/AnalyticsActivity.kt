package com.quiznova.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiznova.admin.databinding.ActivityAnalyticsBinding
import com.quiznova.admin.firebase.CategoryHelper

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val db = FirebaseFirestore.getInstance()
    private var categoryNames = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Analytics"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        loadCategoriesThenData()

        binding.btnCleanupLeaderboard.setOnClickListener {
            cleanupLeaderboard()
        }
    }

    private fun loadCategoriesThenData() {
        CategoryHelper.clearCache()
        CategoryHelper.getCategoryNames { names ->
            runOnUiThread {
                categoryNames = names
                loadAllData()
            }
        }
    }

    private fun loadAllData() {
        loadOverview()
        loadQuestionsByCategory()
        loadQuestionsByDifficulty()
        loadTopPerformers()
        loadWithdrawalSummary()
        loadRecentLogs()
    }

    // ========== OVERVIEW ==========
    private fun loadOverview() {
        db.collection("users").get().addOnSuccessListener { snapshot ->
            binding.tvTotalUsers.text = snapshot.size().toString()

            var premium = 0
            var totalCoins = 0
            var totalQuizzes = 0

            for (doc in snapshot.documents) {
                if (doc.getBoolean("isPremium") == true) premium++
                totalCoins += doc.getLong("totalCoins")?.toInt() ?: 0
                totalQuizzes += doc.getLong("quizzesPlayed")?.toInt() ?: 0
            }

            binding.tvPremiumUsers.text = premium.toString()
            binding.tvTotalCoinsEarned.text = formatNumber(totalCoins)
            binding.tvTotalQuizzes.text = formatNumber(totalQuizzes)
        }

        // Questions count - dynamic
        var totalQ = 0
        var qLoaded = 0
        val qTotal = categoryNames.size

        if (qTotal == 0) {
            binding.tvTotalQuestions.text = "0"
            return
        }

        for (cat in categoryNames) {
            db.collection("questions").document(cat).collection("items").get()
                .addOnSuccessListener { qs ->
                    totalQ += qs.size()
                    qLoaded++
                    if (qLoaded >= qTotal) {
                        runOnUiThread { binding.tvTotalQuestions.text = totalQ.toString() }
                    }
                }
                .addOnFailureListener {
                    qLoaded++
                    if (qLoaded >= qTotal) {
                        runOnUiThread { binding.tvTotalQuestions.text = totalQ.toString() }
                    }
                }
        }

        db.collection("withdrawals")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvPendingWithdrawals.text = snapshot.size().toString()
            }
    }

    // ========== QUESTIONS BY CATEGORY ==========
    private fun loadQuestionsByCategory() {
        binding.layoutCategories.removeAllViews()
        val counts = mutableMapOf<String, Int>()
        var totalQuestions = 0
        var loaded = 0

        if (categoryNames.isEmpty()) return

        for (cat in categoryNames) {
            db.collection("questions").document(cat).collection("items").get()
                .addOnSuccessListener { qs ->
                    counts[cat] = qs.size()
                    totalQuestions += qs.size()
                    loaded++
                    if (loaded >= categoryNames.size) {
                        showCategoryBars(counts, totalQuestions)
                    }
                }
                .addOnFailureListener {
                    counts[cat] = 0
                    loaded++
                    if (loaded >= categoryNames.size) {
                        showCategoryBars(counts, totalQuestions)
                    }
                }
        }
    }

    private fun showCategoryBars(counts: Map<String, Int>, total: Int) {
        runOnUiThread {
            for ((cat, count) in counts.entries.sortedByDescending { it.value }) {
                val percent = if (total > 0) (count * 100 / total) else 0

                val row = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    orientation = LinearLayout.VERTICAL
                }

                val header = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val nameTv = TextView(this).apply {
                    text = cat
                    setTextColor(getColor(R.color.text_primary))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countTv = TextView(this).apply {
                    text = "$count ($percent%)"
                    setTextColor(getColor(R.color.accent_gold))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }

                header.addView(nameTv)
                header.addView(countTv)
                row.addView(header)

                val barBg = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 8
                    ).apply { topMargin = 6 }
                    setBackgroundColor(Color.parseColor("#1A2A3A"))
                }

                val barFill = View(this).apply {
                    val widthPercent = if (total > 0) count.toFloat() / total else 0f
                    layoutParams = LinearLayout.LayoutParams(0, 8, widthPercent.coerceAtLeast(0.01f))
                    setBackgroundColor(getColor(R.color.accent_gold))
                }

                barBg.addView(barFill)
                row.addView(barBg)
                binding.layoutCategories.addView(row)
            }
        }
    }

    // ========== QUESTIONS BY DIFFICULTY ==========
    private fun loadQuestionsByDifficulty() {
        binding.layoutDifficulty.removeAllViews()
        val diffCounts = mutableMapOf("easy" to 0, "medium" to 0, "hard" to 0)
        var loaded = 0

        if (categoryNames.isEmpty()) return

        for (cat in categoryNames) {
            db.collection("questions").document(cat).collection("items").get()
                .addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        val diff = doc.getString("difficulty") ?: "easy"
                        diffCounts[diff] = (diffCounts[diff] ?: 0) + 1
                    }
                    loaded++
                    if (loaded >= categoryNames.size) {
                        showDifficultyBars(diffCounts)
                    }
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded >= categoryNames.size) {
                        showDifficultyBars(diffCounts)
                    }
                }
        }
    }

    private fun showDifficultyBars(counts: Map<String, Int>) {
        runOnUiThread {
            val total = counts.values.sum()

            val diffColors = mapOf(
                "easy" to getColor(R.color.diff_easy),
                "medium" to getColor(R.color.diff_medium),
                "hard" to getColor(R.color.diff_hard)
            )

            for ((diff, count) in counts.entries.sortedByDescending { it.value }) {
                val percent = if (total > 0) (count * 100 / total) else 0

                val row = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    orientation = LinearLayout.VERTICAL
                }

                val header = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val nameTv = TextView(this).apply {
                    text = diff.replaceFirstChar { it.uppercase() }
                    setTextColor(getColor(R.color.text_primary))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countTv = TextView(this).apply {
                    text = "$count ($percent%)"
                    setTextColor(diffColors[diff] ?: getColor(R.color.accent_gold))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }

                header.addView(nameTv)
                header.addView(countTv)
                row.addView(header)

                val barBg = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 8
                    ).apply { topMargin = 6 }
                    setBackgroundColor(Color.parseColor("#1A2A3A"))
                }

                val barFill = View(this).apply {
                    val widthPercent = if (total > 0) count.toFloat() / total else 0f
                    layoutParams = LinearLayout.LayoutParams(0, 8, widthPercent.coerceAtLeast(0.01f))
                    setBackgroundColor(diffColors[diff] ?: getColor(R.color.accent_gold))
                }

                barBg.addView(barFill)
                row.addView(barBg)
                binding.layoutDifficulty.addView(row)
            }
        }
    }

    // ========== TOP PERFORMERS ==========
    private fun loadTopPerformers() {
        binding.layoutTopUsers.removeAllViews()

        db.collection("users")
            .orderBy("totalScore", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                runOnUiThread {
                    if (snapshot.isEmpty) {
                        val noData = TextView(this).apply {
                            text = "No users yet"
                            setTextColor(getColor(R.color.text_muted))
                            textSize = 14f
                            gravity = Gravity.CENTER
                            setPadding(0, 20, 0, 20)
                        }
                        binding.layoutTopUsers.addView(noData)
                        return@runOnUiThread
                    }

                    var rank = 1
                    for (doc in snapshot.documents) {
                        val name = doc.getString("username") ?: "Unknown"
                        val score = doc.getLong("totalScore")?.toInt() ?: 0
                        val coins = doc.getLong("totalCoins")?.toInt() ?: 0
                        val quizzes = doc.getLong("quizzesPlayed")?.toInt() ?: 0

                        val medal = when (rank) {
                            1 -> "1st"
                            2 -> "2nd"
                            3 -> "3rd"
                            else -> "#$rank"
                        }

                        val row = LinearLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 8 }
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(12, 8, 12, 8)
                        }

                        val rankTv = TextView(this).apply {
                            text = medal
                            setTextColor(getColor(R.color.accent_gold))
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT)
                        }

                        val infoLayout = LinearLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            orientation = LinearLayout.VERTICAL
                        }

                        val nameTv = TextView(this).apply {
                            text = name
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                        }

                        val detailTv = TextView(this).apply {
                            text = "Coins: $coins  |  Quizzes: $quizzes"
                            setTextColor(getColor(R.color.text_muted))
                            textSize = 11f
                        }

                        infoLayout.addView(nameTv)
                        infoLayout.addView(detailTv)

                        val scoreTv = TextView(this).apply {
                            text = formatNumber(score)
                            setTextColor(getColor(R.color.accent_gold))
                            textSize = 16f
                            typeface = Typeface.DEFAULT_BOLD
                            gravity = Gravity.CENTER
                            setPadding(12, 0, 0, 0)
                        }

                        row.addView(rankTv)
                        row.addView(infoLayout)
                        row.addView(scoreTv)
                        binding.layoutTopUsers.addView(row)

                        if (rank < 5) {
                            val divider = View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                                ).apply { setMargins(0, 4, 0, 4) }
                                setBackgroundColor(Color.parseColor("#2A3A4E"))
                            }
                            binding.layoutTopUsers.addView(divider)
                        }

                        rank++
                    }
                }
            }
    }

    // ========== WITHDRAWAL SUMMARY ==========
    private fun loadWithdrawalSummary() {
        db.collection("withdrawals").get()
            .addOnSuccessListener { snapshot ->
                var totalAmount = 0
                var approvedAmount = 0
                var pendingAmount = 0

                for (doc in snapshot.documents) {
                    val amount = doc.getLong("amount")?.toInt() ?: 0
                    val status = doc.getString("status") ?: ""

                    totalAmount += amount
                    when (status) {
                        "approved" -> approvedAmount += amount
                        "pending" -> pendingAmount += amount
                    }
                }

                runOnUiThread {
                    binding.tvWithdrawalTotal.text = "${formatNumber(totalAmount)} coins"
                    binding.tvWithdrawalApproved.text = "${formatNumber(approvedAmount)} coins"
                    binding.tvWithdrawalPending.text = "${formatNumber(pendingAmount)} coins"
                }
            }
    }

    // ========== RECENT LOGS ==========
    private fun loadRecentLogs() {
        binding.layoutLogs.removeAllViews()

        db.collection("admin")
            .document("logs")
            .collection("entries")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(15)
            .get()
            .addOnSuccessListener { snapshot ->
                runOnUiThread {
                    if (snapshot.isEmpty) {
                        val noData = TextView(this).apply {
                            text = "No admin actions yet"
                            setTextColor(getColor(R.color.text_muted))
                            textSize = 14f
                            gravity = Gravity.CENTER
                            setPadding(0, 20, 0, 20)
                        }
                        binding.layoutLogs.addView(noData)
                        return@runOnUiThread
                    }

                    for (doc in snapshot.documents) {
                        val details = doc.getString("details") ?: ""
                        val email = doc.getString("adminEmail") ?: ""
                        val time = doc.getTimestamp("timestamp")?.toDate()?.toString()?.take(16) ?: ""

                        val row = LinearLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 8 }
                            orientation = LinearLayout.VERTICAL
                        }

                        val detailsTv = TextView(this).apply {
                            text = "- $details"
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 13f
                        }

                        val timeTv = TextView(this).apply {
                            text = "  $time | $email"
                            setTextColor(getColor(R.color.text_muted))
                            textSize = 11f
                        }

                        row.addView(detailsTv)
                        row.addView(timeTv)
                        binding.layoutLogs.addView(row)

                        val divider = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1
                            ).apply { setMargins(0, 4, 0, 4) }
                            setBackgroundColor(Color.parseColor("#2A3A4E"))
                        }
                        binding.layoutLogs.addView(divider)
                    }
                }
            }
    }

    private fun formatNumber(num: Int): String {
        return when {
            num >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", num / 1_000.0)
            else -> num.toString()
        }
    }

    private fun cleanupLeaderboard() {
        binding.btnCleanupLeaderboard.isEnabled = false
        binding.btnCleanupLeaderboard.text = "CLEANING UP..."

        db.collection("users")
            .whereEqualTo("status", "banned")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "No banned users found", Toast.LENGTH_SHORT).show()
                    resetCleanupButton()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "totalScore", 0L)
                }

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_cleanup_success), Toast.LENGTH_SHORT).show()
                    resetCleanupButton()
                    loadAllData() // Refresh stats
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Batch failed: ${e.message}", Toast.LENGTH_LONG).show()
                    resetCleanupButton()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fetch failed: ${e.message}", Toast.LENGTH_LONG).show()
                resetCleanupButton()
            }
    }

    private fun resetCleanupButton() {
        binding.btnCleanupLeaderboard.isEnabled = true
        binding.btnCleanupLeaderboard.text = getString(R.string.btn_cleanup_leaderboard)
    }
}