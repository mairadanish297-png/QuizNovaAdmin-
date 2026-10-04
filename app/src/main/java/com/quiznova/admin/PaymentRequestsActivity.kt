package com.quiznova.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityPaymentRequestsBinding

data class PaymentRequestAdmin(
    val docId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val planType: String = "",
    val amount: Long = 0,
    val paymentMethod: String = "",
    val transactionId: String = "",
    val accountNumber: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)

class PaymentRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentRequestsBinding
    private val db = FirebaseFirestore.getInstance()
    private val requests = mutableListOf<PaymentRequestAdmin>()
    private lateinit var adapter: PaymentRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Payment Requests"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = PaymentRequestAdapter(
            requests,
            onApprove = { req -> approvePayment(req) },
            onReject = { req -> rejectPayment(req) }
        )

        binding.rvPayments.layoutManager = LinearLayoutManager(this)
        binding.rvPayments.adapter = adapter

        binding.btnFilterPending.setOnClickListener { loadRequests("pending") }
        binding.btnFilterAll.setOnClickListener { loadRequests("all") }

        loadRequests("pending")
    }

    // ═══════════════════════════════════════
    //  LOAD REQUESTS
    // ═══════════════════════════════════════

    private fun extractLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            is Timestamp -> value.toDate().time
            else -> 0L
        }
    }

    private fun loadRequests(filter: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        val collectionRef = db.collection("payment_requests")
        val query = if (filter == "pending") {
            collectionRef.whereEqualTo("status", "pending")
        } else {
            collectionRef.limit(100)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                requests.clear()

                for (doc in snapshot.documents) {
                    requests.add(
                        PaymentRequestAdmin(
                            docId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            userName = doc.getString("userName") ?: "",
                            planType = doc.getString("planType") ?: "",
                            amount = extractLong(doc.get("amount")),
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            transactionId = doc.getString("transactionId") ?: "",
                            accountNumber = doc.getString("accountNumber") ?: "",
                            status = doc.getString("status") ?: "pending",
                            createdAt = extractLong(doc.get("createdAt") ?: doc.get("timestamp"))
                        )
                    )
                }

                if (filter != "pending") {
                    requests.sortByDescending { it.createdAt }
                }

                adapter.notifyDataSetChanged()
                binding.tvCount.text = "${requests.size} requests"
                binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ═══════════════════════════════════════
    //  APPROVE PAYMENT
    // ═══════════════════════════════════════

    private fun approvePayment(req: PaymentRequestAdmin) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Approve Payment?")
            .setMessage(
                "User: ${req.userEmail}\n" +
                        "Plan: ${req.planType}\n" +
                        "Amount: Rs. ${req.amount / 100}\n" +
                        "Method: ${req.paymentMethod.uppercase()}\n" +
                        "TXN: ${req.transactionId}\n\n" +
                        "This will activate their subscription immediately."
            )
            .setPositiveButton("Approve") { _, _ ->
                activateSubscription(req)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun activateSubscription(req: PaymentRequestAdmin) {
        binding.progressBar.visibility = View.VISIBLE

        val days = when (req.planType) {
            "1day" -> 1
            "7days" -> 7
            "30days" -> 30
            else -> 1
        }

        val now = System.currentTimeMillis()
        val expiry = now + (days.toLong() * 24 * 60 * 60 * 1000)

        val subData = hashMapOf(
            "planType" to req.planType,
            "startDate" to now,
            "expiryDate" to expiry,
            "purchaseMethod" to "money_${req.paymentMethod}",
            "amountPaid" to req.amount,
            "transactionId" to req.transactionId,
            "approvedBy" to "admin",
            "purchasedAt" to Timestamp.now()
        )

        db.collection("users")
            .document(req.userId)
            .collection("meta")
            .document("subscription")
            .set(subData)
            .addOnSuccessListener {
                // Update request status to approved
                db.collection("payment_requests")
                    .document(req.docId)
                    .update(
                        mapOf(
                            "status" to "approved",
                            "resolvedAt" to Timestamp.now()
                        )
                    )
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Approved! Subscription activated.", Toast.LENGTH_SHORT).show()
                        loadRequests("pending")
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ═══════════════════════════════════════
    //  REJECT PAYMENT
    // ═══════════════════════════════════════

    private fun rejectPayment(req: PaymentRequestAdmin) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Reject Payment?")
            .setMessage(
                "User: ${req.userEmail}\n" +
                        "TXN: ${req.transactionId}\n\n" +
                        "Reject this payment request?"
            )
            .setPositiveButton("Reject") { _, _ ->
                db.collection("payment_requests")
                    .document(req.docId)
                    .update(
                        mapOf(
                            "status" to "rejected",
                            "resolvedAt" to Timestamp.now()
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
                        loadRequests("pending")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ═══════════════════════════════════════
//  ADAPTER
// ═══════════════════════════════════════

class PaymentRequestAdapter(
    private val requests: List<PaymentRequestAdmin>,
    private val onApprove: (PaymentRequestAdmin) -> Unit,
    private val onReject: (PaymentRequestAdmin) -> Unit
) : RecyclerView.Adapter<PaymentRequestAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvPayUser)
        val tvPlan: TextView = view.findViewById(R.id.tvPayPlan)
        val tvMethod: TextView = view.findViewById(R.id.tvPayMethod)
        val tvTxnId: TextView = view.findViewById(R.id.tvPayTxnId)
        val tvAccount: TextView = view.findViewById(R.id.tvPayAccount)
        val tvStatus: TextView = view.findViewById(R.id.tvPayStatus)
        val btnApprove: TextView = view.findViewById(R.id.btnApprove)
        val btnReject: TextView = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_request, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val req = requests[position]

        holder.tvUser.text = "${req.userName}\n${req.userEmail}"
        holder.tvPlan.text = "${req.planType.uppercase()} — Rs. ${req.amount / 100}"
        holder.tvMethod.text = req.paymentMethod.uppercase()
        holder.tvTxnId.text = "TXN: ${req.transactionId}"
        holder.tvAccount.text = "From: ${req.accountNumber}"
        holder.tvStatus.text = req.status.uppercase()

        val statusColor = when (req.status) {
            "pending" -> ContextCompat.getColor(holder.itemView.context, R.color.status_warning)
            "approved" -> ContextCompat.getColor(holder.itemView.context, R.color.status_success)
            "rejected" -> ContextCompat.getColor(holder.itemView.context, R.color.status_error)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.text_muted)
        }
        holder.tvStatus.setTextColor(statusColor)

        if (req.status == "pending") {
            holder.btnApprove.visibility = View.VISIBLE
            holder.btnReject.visibility = View.VISIBLE
        } else {
            holder.btnApprove.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
        }

        holder.btnApprove.setOnClickListener { onApprove(req) }
        holder.btnReject.setOnClickListener { onReject(req) }
    }

    override fun getItemCount() = requests.size
}