package com.quiznova.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiznova.admin.databinding.ActivityManageWithdrawalsBinding

class ManageWithdrawalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageWithdrawalsBinding
    private val db = FirebaseFirestore.getInstance()
    private val withdrawals = mutableListOf<WithdrawalItem>()
    private lateinit var adapter: WithdrawalAdapter
    private var currentTab = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageWithdrawalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_withdrawals)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = WithdrawalAdapter(withdrawals,
            onApprove = { w -> approveWithdrawal(w) },
            onReject = { w -> rejectWithdrawal(w) }
        )

        binding.rvWithdrawals.layoutManager = LinearLayoutManager(this)
        binding.rvWithdrawals.adapter = adapter

        binding.tabPending.setOnClickListener { switchTab("pending") }
        binding.tabApproved.setOnClickListener { switchTab("approved") }
        binding.tabRejected.setOnClickListener { switchTab("rejected") }

        loadWithdrawals()
    }

    override fun onResume() {
        super.onResume()
        loadWithdrawals()
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        val gold = ContextCompat.getColor(this, R.color.accent_gold)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        binding.tabPending.setTextColor(if (tab == "pending") gold else muted)
        binding.tabApproved.setTextColor(if (tab == "approved") gold else muted)
        binding.tabRejected.setTextColor(if (tab == "rejected") gold else muted)

        loadWithdrawals()
    }

    private fun loadWithdrawals() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("withdrawals")
            .whereEqualTo("status", currentTab)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                withdrawals.clear()
                for (doc in snapshot.documents) {
                    withdrawals.add(
                        WithdrawalItem(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "Unknown",
                            email = doc.getString("email") ?: "",
                            amount = (doc.getLong("amount")?.toInt() ?: 0),
                            dollarAmount = doc.getDouble("dollarAmount") ?: 0.0,
                            method = doc.getString("method") ?: "",
                            accountNumber = doc.getString("accountNumber") ?: "",
                            status = doc.getString("status") ?: "pending",
                            requestedAt = doc.getTimestamp("requestedAt")?.toDate()?.toString() ?: ""
                        )
                    )
                }
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = if (withdrawals.isEmpty()) View.VISIBLE else View.GONE
                adapter.notifyDataSetChanged()

                val total = withdrawals.sumOf { it.amount }
                binding.tvTotalAmount.text = getString(R.string.format_total_amount, total.toString() + getString(R.string.symbol_coin_gold_suffix))
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun approveWithdrawal(w: WithdrawalItem) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Approve Withdrawal?")
            .setMessage("Send ${w.dollarAmount}$ to ${w.username}\nMethod: ${w.method}\nAccount: ${w.accountNumber}")
            .setPositiveButton("Approve") { _, _ ->
                db.collection("withdrawals").document(w.id)
                    .update(
                        "status", "approved",
                        "processedAt", com.google.firebase.Timestamp.now(),
                        "processedBy", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Withdrawal approved!", Toast.LENGTH_SHORT).show()
                        loadWithdrawals()
                        logAction("Approved withdrawal ${w.id} for ${w.username}: ${w.amount} coins")
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectWithdrawal(w: WithdrawalItem) {
        val input = android.widget.EditText(this).apply {
            hint = "Rejection reason"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Reject Withdrawal?")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim().ifEmpty { "No reason" }

                db.collection("withdrawals").document(w.id)
                    .update(
                        "status", "rejected",
                        "rejectionReason", reason,
                        "processedAt", com.google.firebase.Timestamp.now(),
                        "processedBy", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
                    )
                    .addOnSuccessListener {
                        // Refund coins to user
                        db.collection("users").document(w.userId)
                            .update("totalCoins", com.google.firebase.firestore.FieldValue.increment(w.amount.toLong()))

                        Toast.makeText(this, "❌ Rejected. Coins refunded.", Toast.LENGTH_SHORT).show()
                        loadWithdrawals()
                        logAction("Rejected withdrawal ${w.id} for ${w.username}: $reason")
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logAction(details: String) {
        val log = hashMapOf(
            "action" to "withdrawal",
            "details" to details,
            "adminEmail" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("admin").document("logs").collection("entries").add(log)
    }
}

data class WithdrawalItem(
    val id: String,
    val userId: String,
    val username: String,
    val email: String,
    val amount: Int,
    val dollarAmount: Double,
    val method: String,
    val accountNumber: String,
    val status: String,
    val requestedAt: String
)

class WithdrawalAdapter(
    private val items: List<WithdrawalItem>,
    private val onApprove: (WithdrawalItem) -> Unit,
    private val onReject: (WithdrawalItem) -> Unit
) : RecyclerView.Adapter<WithdrawalAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDollar: TextView = view.findViewById(R.id.tvDollar)
        val tvMethod: TextView = view.findViewById(R.id.tvMethod)
        val tvAccount: TextView = view.findViewById(R.id.tvAccount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnApprove: View = view.findViewById(R.id.btnApprove)
        val btnReject: View = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_withdrawal, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val w = items[position]
        val context = holder.itemView.context

        holder.tvUsername.text = w.username
        holder.tvAmount.text = context.getString(R.string.format_coins_val, w.amount)
        holder.tvDollar.text = context.getString(R.string.format_dollar_val, w.dollarAmount)
        holder.tvMethod.text = w.method
        holder.tvAccount.text = w.accountNumber
        holder.tvDate.text = w.requestedAt

        when (w.status) {
            "pending" -> {
                holder.tvStatus.text = context.getString(R.string.status_pending_caps)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                holder.layoutActions.visibility = View.VISIBLE
            }
            "approved" -> {
                holder.tvStatus.text = context.getString(R.string.status_approved_caps)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_success))
                holder.layoutActions.visibility = View.GONE
            }
            "rejected" -> {
                holder.tvStatus.text = context.getString(R.string.status_rejected_caps)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_error_light))
                holder.layoutActions.visibility = View.GONE
            }
        }

        holder.btnApprove.setOnClickListener { onApprove(w) }
        holder.btnReject.setOnClickListener { onReject(w) }
    }

    override fun getItemCount() = items.size
}