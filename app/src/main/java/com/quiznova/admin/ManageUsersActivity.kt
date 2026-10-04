package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityManageUsersBinding

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private val db = FirebaseFirestore.getInstance()
    private val allUsers = mutableListOf<UserItem>()
    private val displayedUsers = mutableListOf<UserItem>()
    private lateinit var adapter: UserListAdapter
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_manage_users)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        currentFilter = intent.getStringExtra("filter") ?: "all"
        if (currentFilter != "all") {
            binding.tvFilter.text = currentFilter.replaceFirstChar { it.uppercase() }
        }

        adapter = UserListAdapter { user ->
            val intent = Intent(this, UserDetailsActivity::class.java)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })

        binding.tvFilter.setOnClickListener {
            val options = arrayOf(
                getString(R.string.filter_all),
                getString(R.string.filter_active),
                getString(R.string.filter_banned),
                getString(R.string.filter_premium),
                getString(R.string.filter_highscore)
            )
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.filter_title))
                .setItems(options) { _, which ->
                    currentFilter = when (which) {
                        1 -> "active"
                        2 -> "banned"
                        3 -> "premium"
                        4 -> "highscore"
                        else -> "all"
                    }
                    binding.tvFilter.text = options[which]
                    applyFilter()
                }
                .show()
        }

        loadUsers()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                allUsers.clear()
                for (doc in snapshot.documents) {
                    allUsers.add(
                        UserItem(
                            uid = doc.id,
                            username = doc.getString("username") ?: "Unknown",
                            email = doc.getString("email") ?: "",
                            totalScore = (doc.getLong("totalScore")?.toInt() ?: 0),
                            totalCoins = (doc.getLong("totalCoins")?.toInt() ?: 0),
                            quizzesPlayed = (doc.getLong("quizzesPlayed")?.toInt() ?: 0),
                            status = doc.getString("status") ?: "active",
                            isPremium = doc.getBoolean("isPremium") ?: false
                        )
                    )
                }
                binding.progressBar.visibility = View.GONE
                updateStats()
                applyFilter()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun updateStats() {
        binding.tvUserCount.text = getString(R.string.format_users_count, allUsers.size)
        binding.tvActiveCount.text = getString(R.string.format_active_count, allUsers.count { it.status == "active" })
        binding.tvBannedCount.text = getString(R.string.format_banned_count, allUsers.count { it.status == "banned" })
    }

    private fun applyFilter() {
        val search = binding.etSearch.text.toString().trim().lowercase()

        val filteredList = allUsers.filter { user ->
            val matchesSearch = search.isEmpty() ||
                    user.username.lowercase().contains(search) ||
                    user.email.lowercase().contains(search)

            val matchesFilter = when (currentFilter) {
                "active" -> user.status == "active"
                "banned" -> user.status == "banned"
                "premium" -> user.isPremium
                "highscore" -> user.totalScore >= 1000
                else -> true
            }

            matchesSearch && matchesFilter
        }

        adapter.submitList(filteredList)
        binding.tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }
}

data class UserItem(
    val uid: String,
    val username: String,
    val email: String,
    val totalScore: Int,
    val totalCoins: Int,
    val quizzesPlayed: Int,
    val status: String,
    val isPremium: Boolean
)

class UserListAdapter(
    private val onClick: (UserItem) -> Unit
) : RecyclerView.Adapter<UserListAdapter.VH>() {

    private var users: List<UserItem> = emptyList()

    fun submitList(newList: List<UserItem>) {
        val diffCallback = UserDiffCallback(users, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        users = newList
        diffResult.dispatchUpdatesTo(this)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvCoins: TextView = view.findViewById(R.id.tvCoins)
        val tvQuizzes: TextView = view.findViewById(R.id.tvQuizzes)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        val context = holder.itemView.context

        holder.tvAvatar.text = if (user.isPremium) context.getString(R.string.symbol_premium) else context.getString(R.string.symbol_user)
        holder.tvUsername.text = user.username
        holder.tvEmail.text = user.email
        holder.tvCoins.text = context.getString(R.string.format_coins, user.totalCoins)
        holder.tvQuizzes.text = context.getString(R.string.format_quizzes, user.quizzesPlayed)

        when {
            user.status == "banned" -> {
                holder.tvStatus.text = context.getString(R.string.status_banned)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_error))
            }
            user.isPremium -> {
                holder.tvStatus.text = context.getString(R.string.status_premium)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_info))
            }
            else -> {
                holder.tvStatus.text = context.getString(R.string.status_active)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_success))
            }
        }

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size

    class UserDiffCallback(
        private val oldList: List<UserItem>,
        private val newList: List<UserItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].uid == newList[newItemPosition].uid
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}