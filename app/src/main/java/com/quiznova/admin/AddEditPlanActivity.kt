package com.quiznova.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityAddEditPlanBinding

class AddEditPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPlanBinding
    private val db = FirebaseFirestore.getInstance()
    private var planId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planId = intent.getStringExtra("planId")
        binding.includeHeader.tvTitle.text = if (planId == null) "Add Plan" else "Edit Plan"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        if (planId != null) {
            binding.etPlanType.setText(intent.getStringExtra("type"))
            binding.etLabel.setText(intent.getStringExtra("label"))
            binding.etDays.setText(intent.getIntExtra("days", 0).toString())
            binding.etCoinPrice.setText(intent.getIntExtra("coinPrice", 0).toString())
            binding.etMoneyPrice.setText(intent.getStringExtra("moneyPrice"))
            binding.etMoneyPriceRaw.setText(intent.getLongExtra("moneyPriceRaw", 0L).toString())
        }

        binding.btnSave.setOnClickListener { savePlan() }
    }

    private fun savePlan() {
        val type = binding.etPlanType.text.toString().trim()
        val label = binding.etLabel.text.toString().trim()
        val days = binding.etDays.text.toString().toIntOrNull() ?: 0
        val coinPrice = binding.etCoinPrice.text.toString().toIntOrNull() ?: 0
        val moneyPrice = binding.etMoneyPrice.text.toString().trim()
        val moneyPriceRaw = binding.etMoneyPriceRaw.text.toString().toLongOrNull() ?: 0L

        if (type.isEmpty() || label.isEmpty() || moneyPrice.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "type" to type,
            "label" to label,
            "days" to days,
            "coinPrice" to coinPrice,
            "moneyPrice" to moneyPrice,
            "moneyPriceRaw" to moneyPriceRaw
        )

        val docRef = if (planId == null) {
            db.collection("plans").document()
        } else {
            db.collection("plans").document(planId!!)
        }

        docRef.set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Plan saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
