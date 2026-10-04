package com.quiznova.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val notifications = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    private var targetType = "all" // all, single, premium

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_notifications)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = NotificationAdapter()
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        setupTabs()
        setupSend()
        loadHistory()
    }

    private fun setupTabs() {
        val gold = ContextCompat.getColor(this, R.color.accent_gold)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        fun updateTabs() {
            binding.tabAll.setTextColor(if (targetType == "all") gold else muted)
            binding.tabSingle.setTextColor(if (targetType == "single") gold else muted)
            binding.tabPremium.setTextColor(if (targetType == "premium") gold else muted)
            binding.etUserId.visibility = if (targetType == "single") View.VISIBLE else View.GONE
        }

        binding.tabAll.setOnClickListener {
            targetType = "all"
            updateTabs()
        }

        binding.tabSingle.setOnClickListener {
            targetType = "single"
            updateTabs()
        }

        binding.tabPremium.setOnClickListener {
            targetType = "premium"
            updateTabs()
        }
    }

    private fun setupSend() {
        binding.btnSend.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val message = binding.etMessage.text.toString().trim()
            val imageUrl = binding.etImageUrl.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_enter_title), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_enter_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (targetType) {
                "all" -> sendToAll(title, message, imageUrl)
                "single" -> sendToSingle(title, message, imageUrl)
                "premium" -> sendToPremium(title, message, imageUrl)
            }
        }
    }

    private fun sendToAll(title: String, message: String, imageUrl: String) {
        binding.btnSend.isEnabled = false
        binding.btnSend.text = getString(R.string.status_sending)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
        val now = Timestamp.now()

        // Save notification record (Root collection - usually what home screen reads)
        val notificationData = hashMapOf(
            "userId" to "all",
            "title" to title,
            "message" to message,
            "imageUrl" to imageUrl,
            "type" to "all",
            "sentBy" to email,
            "isActive" to true,
            "createdAt" to now,
            "timestamp" to now.toDate().time
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                // Now add to each user's notification subcollection
                db.collection("users").get()
                    .addOnSuccessListener { users ->
                        var sent = 0
                        val total = users.size()

                        if (total == 0) {
                            onSendComplete(getString(R.string.msg_sent_0))
                            return@addOnSuccessListener
                        }

                        for (user in users.documents) {
                            val userNotif = hashMapOf(
                                "title" to title,
                                "message" to message,
                                "imageUrl" to imageUrl,
                                "type" to "all",
                                "read" to false,
                                "sentBy" to email,
                                "isActive" to true,
                                "createdAt" to now,
                                "timestamp" to now.toDate().time
                            )

                            db.collection("users")
                                .document(user.id)
                                .collection("notifications")
                                .add(userNotif)
                                .addOnSuccessListener {
                                    sent++
                                    if (sent >= total) onSendComplete(getString(R.string.format_sent_users, sent))
                                }
                                .addOnFailureListener {
                                    sent++
                                    if (sent >= total) onSendComplete(getString(R.string.format_sent_users_partial, sent))
                                }
                        }
                    }
            }
            .addOnFailureListener { e ->
                binding.btnSend.isEnabled = true
                binding.btnSend.text = getString(R.string.btn_send_notif)
                Toast.makeText(this, getString(R.string.error_config_save, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun sendToSingle(title: String, message: String, imageUrl: String) {
        val userId = binding.etUserId.text.toString().trim()
        if (userId.isEmpty()) {
            Toast.makeText(this, getString(R.string.hint_user_id), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSend.isEnabled = false
        binding.btnSend.text = getString(R.string.status_sending)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
        val now = Timestamp.now()

        val notificationData = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "imageUrl" to imageUrl,
            "type" to "single",
            "sentBy" to email,
            "isActive" to true,
            "createdAt" to now,
            "timestamp" to now.toDate().time
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                // Add to user's notifications
                val userNotif = hashMapOf(
                    "title" to title,
                    "message" to message,
                    "imageUrl" to imageUrl,
                    "type" to "single",
                    "read" to false,
                    "sentBy" to email,
                    "isActive" to true,
                    "createdAt" to now,
                    "timestamp" to now.toDate().time
                )

                db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .add(userNotif)
                    .addOnSuccessListener {
                        onSendComplete(getString(R.string.msg_sent_user, userId))
                    }
                    .addOnFailureListener { e ->
                        onSendComplete(getString(R.string.msg_sent_user_partial, e.message))
                    }
            }
            .addOnFailureListener { e ->
                binding.btnSend.isEnabled = true
                binding.btnSend.text = getString(R.string.btn_send_notif)
                Toast.makeText(this, getString(R.string.error_config_save, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun sendToPremium(title: String, message: String, imageUrl: String) {
        binding.btnSend.isEnabled = false
        binding.btnSend.text = getString(R.string.status_sending)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
        val now = Timestamp.now()

        val notificationData = hashMapOf(
            "userId" to "premium",
            "title" to title,
            "message" to message,
            "imageUrl" to imageUrl,
            "type" to "premium",
            "sentBy" to email,
            "isActive" to true,
            "createdAt" to now,
            "timestamp" to now.toDate().time
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                db.collection("users")
                    .whereEqualTo("isPremium", true)
                    .get()
                    .addOnSuccessListener { users ->
                        if (users.isEmpty) {
                            onSendComplete(getString(R.string.msg_no_premium))
                            return@addOnSuccessListener
                        }

                        var sent = 0
                        val total = users.size()

                        for (user in users.documents) {
                            val userNotif = hashMapOf(
                                "title" to title,
                                "message" to message,
                                "imageUrl" to imageUrl,
                                "type" to "premium",
                                "read" to false,
                                "sentBy" to email,
                                "isActive" to true,
                                "createdAt" to now,
                                "timestamp" to now.toDate().time
                            )

                            db.collection("users")
                                .document(user.id)
                                .collection("notifications")
                                .add(userNotif)
                                .addOnSuccessListener {
                                    sent++
                                    if (sent >= total) onSendComplete(getString(R.string.format_sent_premium, sent))
                                }
                                .addOnFailureListener {
                                    sent++
                                    if (sent >= total) onSendComplete(getString(R.string.format_sent_premium_partial, sent))
                                }
                        }
                    }
            }
            .addOnFailureListener { e ->
                binding.btnSend.isEnabled = true
                binding.btnSend.text = getString(R.string.btn_send_notif)
                Toast.makeText(this, getString(R.string.error_config_save, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun onSendComplete(message: String) {
        runOnUiThread {
            binding.btnSend.isEnabled = true
            binding.btnSend.text = getString(R.string.btn_send_notif)
            binding.etTitle.text.clear()
            binding.etMessage.text.clear()
            binding.etImageUrl.text.clear()
            binding.etUserId.text.clear()
            Toast.makeText(this, getString(R.string.format_sent_success, message), Toast.LENGTH_LONG).show()
            loadHistory()
        }
    }

    // ═══ HISTORY ═══
    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("notifications")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE

                val newList = mutableListOf<NotificationItem>()
                for (doc in snapshot.documents) {
                    val createdAtStr = safeFormatDate(doc.get("createdAt") ?: doc.get("timestamp"))
                    newList.add(
                        NotificationItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "all",
                            sentBy = doc.getString("sentBy") ?: "",
                            time = createdAtStr
                        )
                    )
                }

                adapter.submitList(newList)
                binding.tvEmpty.visibility = if (newList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun safeFormatDate(value: Any?): String {
        if (value == null) return ""
        val millis = when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
        if (millis == 0L) return ""
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
        } catch (_: Exception) {
            ""
        }
    }
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val sentBy: String,
    val time: String
)

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.VH>() {

    private var items: List<NotificationItem> = emptyList()

    fun submitList(newList: List<NotificationItem>) {
        val diffCallback = NotificationDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newList
        diffResult.dispatchUpdatesTo(this)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvSentBy: TextView = view.findViewById(R.id.tvSentBy)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        holder.tvTime.text = item.time
        holder.tvSentBy.text = context.getString(R.string.format_sent_by, item.sentBy)

        when (item.type) {
            "all" -> {
                holder.tvType.text = context.getString(R.string.status_all_caps)
                holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.status_info))
            }
            "single" -> {
                holder.tvType.text = context.getString(R.string.status_single_caps)
                holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
            }
            "premium" -> {
                holder.tvType.text = context.getString(R.string.status_premium)
                holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
            }
            else -> {
                holder.tvType.text = item.type.uppercase()
                holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    override fun getItemCount() = items.size

    class NotificationDiffCallback(
        private val oldList: List<NotificationItem>,
        private val newList: List<NotificationItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}