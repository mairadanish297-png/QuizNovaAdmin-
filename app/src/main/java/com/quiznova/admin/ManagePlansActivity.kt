package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityManagePlansBinding
import com.quiznova.admin.databinding.ItemPlanBinding
import com.quiznova.admin.models.PlanData

class ManagePlansActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagePlansBinding
    private val plans = mutableListOf<PlanData>()
    private lateinit var adapter: PlanListAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Manage Plans"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = PlanListAdapter(plans,
            onEdit = { plan ->
                val intent = Intent(this, AddEditPlanActivity::class.java)
                intent.putExtra("planId", plan.id)
                intent.putExtra("type", plan.type)
                intent.putExtra("label", plan.label)
                intent.putExtra("days", plan.days)
                intent.putExtra("coinPrice", plan.coinPrice)
                intent.putExtra("moneyPrice", plan.moneyPrice)
                intent.putExtra("moneyPriceRaw", plan.moneyPriceRaw)
                startActivity(intent)
            },
            onDelete = { plan -> confirmDelete(plan) }
        )

        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter

        binding.btnAddPlan.setOnClickListener {
            startActivity(Intent(this, AddEditPlanActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("plans").get()
            .addOnSuccessListener { result ->
                plans.clear()
                for (doc in result) {
                    val plan = PlanData(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        label = doc.getString("label") ?: "",
                        days = doc.getLong("days")?.toInt() ?: 0,
                        coinPrice = doc.getLong("coinPrice")?.toInt() ?: 0,
                        moneyPrice = doc.getString("moneyPrice") ?: "",
                        moneyPriceRaw = doc.getLong("moneyPriceRaw") ?: 0L
                    )
                    plans.add(plan)
                }
                binding.progressBar.visibility = View.GONE
                adapter.notifyDataSetChanged()
                binding.tvPlanCount.text = "${plans.size} Plans"
                binding.tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load plans", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(plan: PlanData) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Delete Plan")
            .setMessage("Are you sure you want to delete '${plan.label}'?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("plans").document(plan.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Plan deleted", Toast.LENGTH_SHORT).show()
                        loadPlans()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class PlanListAdapter(
        private val list: List<PlanData>,
        private val onEdit: (PlanData) -> Unit,
        private val onDelete: (PlanData) -> Unit
    ) : RecyclerView.Adapter<PlanListAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemPlanBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val plan = list[position]
            holder.itemBinding.tvPlanLabel.text = plan.label
            holder.itemBinding.tvPlanDetails.text = "${plan.days} Days | type: ${plan.type}"
            holder.itemBinding.tvPlanPriceCoins.text = "${plan.coinPrice} Coins"
            holder.itemBinding.tvPlanPriceMoney.text = plan.moneyPrice

            holder.itemBinding.btnEditPlan.setOnClickListener { onEdit(plan) }
            holder.itemBinding.btnDeletePlan.setOnClickListener { onDelete(plan) }
        }

        override fun getItemCount() = list.size
    }
}
