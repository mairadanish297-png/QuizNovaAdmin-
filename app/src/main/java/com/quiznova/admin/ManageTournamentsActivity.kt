package com.quiznova.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiznova.admin.databinding.ActivityManageTournamentsBinding
import com.quiznova.admin.databinding.ItemTournamentBinding
import java.text.SimpleDateFormat
import java.util.*

class ManageTournamentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTournamentsBinding
    private val db = FirebaseFirestore.getInstance()
    private val calendarStart = Calendar.getInstance()
    private val calendarEnd = Calendar.getInstance()
    private val tournaments = mutableListOf<TournamentItem>()
    private lateinit var adapter: TournamentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTournamentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Tournaments"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupDateTimePickers()
        setupScheduleButton()
        loadTournaments()
    }

    private fun setupRecyclerView() {
        adapter = TournamentAdapter(tournaments) { tournament ->
            confirmAnnounceWinners(tournament)
        }
        binding.rvTournaments.adapter = adapter
    }

    private fun confirmAnnounceWinners(tournament: TournamentItem) {
        AlertDialog.Builder(this)
            .setTitle("Announce Winners?")
            .setMessage("This will close the tournament and distribute the prize pool of Rs. ${tournament.prizePool} to the top 3 players (1st: 50%, 2nd: 30%, 3rd: 20%).")
            .setPositiveButton("Announce") { _, _ ->
                fetchWinnersAndDistribute(tournament)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchWinnersAndDistribute(tournament: TournamentItem) {
        db.collection("tournament_participants")
            .whereEqualTo("tournamentId", tournament.id)
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    Toast.makeText(this, getString(R.string.msg_no_participants), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                val winners = snapshots.documents
                
                // 1st Place (50%)
                if (winners.size >= 1) {
                    val uid = winners[0].getString("userId") ?: ""
                    val amount = tournament.prizePool * 0.5
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "walletBalance", FieldValue.increment(amount))
                }

                // 2nd Place (30%)
                if (winners.size >= 2) {
                    val uid = winners[1].getString("userId") ?: ""
                    val amount = tournament.prizePool * 0.3
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "walletBalance", FieldValue.increment(amount))
                }

                // 3rd Place (20%)
                if (winners.size >= 3) {
                    val uid = winners[2].getString("userId") ?: ""
                    val amount = tournament.prizePool * 0.2
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "walletBalance", FieldValue.increment(amount))
                }

                // Mark tournament closed
                val tournamentRef = db.collection("tournaments").document(tournament.id)
                batch.update(tournamentRef, "status", "closed", "isClosed", true)

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_winners_announced), Toast.LENGTH_LONG).show()
                    loadTournaments()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDateTimePickers() {
        binding.tvStartTime.setOnClickListener {
            showDateTimePicker { cal ->
                calendarStart.time = cal.time
                binding.tvStartTime.text = formatDateTime(cal.time)
            }
        }

        binding.tvEndTime.setOnClickListener {
            showDateTimePicker { cal ->
                calendarEnd.time = cal.time
                binding.tvEndTime.text = formatDateTime(cal.time)
            }
        }
    }

    private fun showDateTimePicker(onSelected: (Calendar) -> Unit) {
        val current = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val date = Calendar.getInstance()
            date.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                date.set(Calendar.HOUR_OF_DAY, hour)
                date.set(Calendar.MINUTE, minute)
                onSelected(date)
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show()
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun formatDateTime(date: Date): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
    }

    private fun setupScheduleButton() {
        binding.btnSchedule.setOnClickListener {
            val title = binding.etTournamentTitle.text.toString().trim()
            val prize = binding.etPrizePool.text.toString().toIntOrNull() ?: 0

            if (title.isEmpty() || prize <= 0 || binding.tvStartTime.text.contains("Select")) {
                Toast.makeText(this, getString(R.string.msg_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf(
                "title" to title,
                "prizePool" to prize,
                "startTime" to Timestamp(calendarStart.time),
                "endTime" to Timestamp(calendarEnd.time),
                "status" to "upcoming",
                "createdAt" to Timestamp.now(),
                "participants" to 0,
                "isClosed" to false
            )

            db.collection("tournaments").add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_tournament_scheduled), Toast.LENGTH_SHORT).show()
                    binding.etTournamentTitle.text.clear()
                    binding.etPrizePool.text.clear()
                    binding.tvStartTime.text = getString(R.string.label_select_start)
                    binding.tvEndTime.text = getString(R.string.label_select_end)
                    loadTournaments()
                }
        }
    }

    private fun loadTournaments() {
        db.collection("tournaments")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                tournaments.clear()
                for (doc in docs) {
                    val item = TournamentItem(
                        doc.id,
                        doc.getString("title") ?: "",
                        doc.getLong("prizePool")?.toInt() ?: 0,
                        doc.getTimestamp("startTime"),
                        doc.getTimestamp("endTime"),
                        doc.getString("status") ?: "upcoming",
                        doc.getBoolean("isClosed") ?: false
                    )
                    tournaments.add(item)
                }
                adapter.notifyDataSetChanged()
            }
    }
}

data class TournamentItem(
    val id: String,
    val title: String,
    val prizePool: Int,
    val start: Timestamp?,
    val end: Timestamp?,
    val status: String,
    val isClosed: Boolean
)

class TournamentAdapter(
    private val items: List<TournamentItem>,
    private val onAction: (TournamentItem) -> Unit
) : RecyclerView.Adapter<TournamentAdapter.VH>() {

    class VH(val binding: ItemTournamentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvPrize.text = "Prize: Rs. ${item.prizePool}"
        holder.binding.tvStatus.text = item.status.uppercase()
        
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val startStr = item.start?.toDate()?.let { sdf.format(it) } ?: ""
        val endStr = item.end?.toDate()?.let { sdf.format(it) } ?: ""
        holder.binding.tvTime.text = "$startStr - $endStr"

        val now = Timestamp.now()
        val hasEnded = item.end?.let { it.seconds < now.seconds } ?: false
        
        if (hasEnded && !item.isClosed) {
            holder.binding.btnAnnounce.visibility = View.VISIBLE
            holder.binding.btnAnnounce.setOnClickListener { onAction(item) }
        } else {
            holder.binding.btnAnnounce.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
