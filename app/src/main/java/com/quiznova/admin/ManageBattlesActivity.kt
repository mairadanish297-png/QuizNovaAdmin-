package com.quiznova.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityManageBattlesBinding
import com.quiznova.admin.databinding.ItemBattleBinding

class ManageBattlesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBattlesBinding
    private val db = FirebaseFirestore.getInstance()
    private val battles = mutableListOf<BattleItem>()
    private lateinit var adapter: BattleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBattlesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Live Battles"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        listenForBattles()
    }

    private fun setupRecyclerView() {
        adapter = BattleAdapter(battles)
        binding.rvBattles.adapter = adapter
    }

    private fun listenForBattles() {
        db.collection("battles")
            .whereEqualTo("status", "in_progress")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                battles.clear()
                snapshots?.forEach { doc ->
                    val item = BattleItem(
                        doc.id,
                        doc.getString("player1Name") ?: "Player 1",
                        doc.getString("player2Name") ?: "Player 2",
                        doc.getLong("score1")?.toInt() ?: 0,
                        doc.getLong("score2")?.toInt() ?: 0,
                        doc.getString("status") ?: "in_progress"
                    )
                    battles.add(item)
                }
                binding.tvActiveBattlesCount.text = getString(R.string.format_active_battles, battles.size)
                adapter.notifyDataSetChanged()
            }
    }
}

data class BattleItem(
    val id: String,
    val player1: String,
    val player2: String,
    val score1: Int,
    val score2: Int,
    val status: String
)

class BattleAdapter(private val items: List<BattleItem>) : RecyclerView.Adapter<BattleAdapter.VH>() {
    class VH(val binding: ItemBattleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemBattleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvPlayer1.text = item.player1
        holder.binding.tvPlayer2.text = item.player2
        holder.binding.tvScore1.text = item.score1.toString()
        holder.binding.tvScore2.text = item.score2.toString()
        holder.binding.tvBattleStatus.text = item.status.uppercase()
    }

    override fun getItemCount() = items.size
}
