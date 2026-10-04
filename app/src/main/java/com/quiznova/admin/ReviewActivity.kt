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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityReviewBinding
import com.quiznova.admin.models.QuestionData
import com.quiznova.admin.models.QuestionLang

class ReviewActivity : AppCompatActivity() {

    companion object {
        var questionsToReview = listOf<QuestionData>()
    }

    private lateinit var binding: ActivityReviewBinding
    private val db = FirebaseFirestore.getInstance()
    private var questions = mutableListOf<QuestionData>()
    private lateinit var adapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Review Questions"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        questions.clear()
        questions.addAll(questionsToReview)

        adapter = ReviewAdapter(questions) { position ->
            questions.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, questions.size - position)
            updateCount()
        }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter
        updateCount()

        binding.btnUploadAll.setOnClickListener { uploadAll() }
        binding.btnUploadBottom.setOnClickListener { uploadAll() }

        binding.btnDiscard.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle("Discard All?")
                .setMessage("Remove all ${questions.size} questions?")
                .setPositiveButton("Discard") { _, _ ->
                    questions.clear()
                    adapter.notifyDataSetChanged()
                    updateCount()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateCount() {
        binding.tvReviewCount.text = "${questions.size} Questions"
    }

    private fun uploadAll() {
        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions to upload", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUploadAll.isEnabled = false
        binding.btnUploadBottom.isEnabled = false
        binding.btnUploadBottom.text = "UPLOADING..."

        val email = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
        var uploaded = 0
        var failed = 0

        for (q in questions) {
            val catName = q.category.ifEmpty { "General Knowledge" }

            val data = hashMapOf(
                "en" to hashMapOf("question" to q.en.question, "options" to q.en.options, "explanation" to q.en.explanation),
                "ur" to hashMapOf("question" to q.ur.question, "options" to q.ur.options, "explanation" to q.ur.explanation),
                "roman" to hashMapOf("question" to q.roman.question, "options" to q.roman.options, "explanation" to q.roman.explanation),
                "correct" to q.correct,
                "difficulty" to q.difficulty,
                "category" to catName,
                "type" to "mcq",
                "tags" to q.tags,
                "hasImage" to false,
                "imageUrl" to "",
                "isActive" to true,
                "timesAnswered" to 0,
                "timesCorrect" to 0,
                "createdAt" to Timestamp.now(),
                "createdBy" to email
            )

            db.collection("questions")
                .document(catName)
                .collection("items")
                .add(data)
                .addOnSuccessListener {
                    uploaded++
                    checkDone(uploaded, failed)
                }
                .addOnFailureListener {
                    failed++
                    checkDone(uploaded, failed)
                }
        }
    }

    private fun checkDone(uploaded: Int, failed: Int) {
        if (uploaded + failed >= questions.size) {
            runOnUiThread {
                binding.btnUploadAll.isEnabled = true
                binding.btnUploadBottom.isEnabled = true
                binding.btnUploadBottom.text = "UPLOAD ALL"

                if (failed == 0) {
                    Toast.makeText(this, "✅ All $uploaded questions uploaded!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Uploaded: $uploaded | Failed: $failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

// ════════════════════════════════════════════
//  REVIEW ADAPTER
// ════════════════════════════════════════════

class ReviewAdapter(
    private val questions: List<QuestionData>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    private val currentLang = mutableMapOf<QuestionData, String>()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNum: TextView = view.findViewById(R.id.tvQuestionNumber)
        val tvDiff: TextView = view.findViewById(R.id.tvDifficulty)
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvOptA: TextView = view.findViewById(R.id.tvOptionA)
        val tvOptB: TextView = view.findViewById(R.id.tvOptionB)
        val tvOptC: TextView = view.findViewById(R.id.tvOptionC)
        val tvOptD: TextView = view.findViewById(R.id.tvOptionD)
        val tvExplanation: TextView = view.findViewById(R.id.tvExplanation)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
        val btnEN: TextView = view.findViewById(R.id.btnLangEN)
        val btnUR: TextView = view.findViewById(R.id.btnLangUR)
        val btnRoman: TextView = view.findViewById(R.id.btnLangRoman)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_review, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = questions[position]
        val lang = currentLang[q] ?: "en"

        holder.tvNum.text = "Q${position + 1}"
        holder.tvDiff.text = q.difficulty.replaceFirstChar { it.uppercase() }

        val diffColor = when (q.difficulty) {
            "easy" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_easy)
            "medium" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_medium)
            "hard" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_hard)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.accent_gold)
        }
        holder.tvDiff.setTextColor(diffColor)

        val langData = when (lang) {
            "ur" -> q.ur
            "roman" -> q.roman
            else -> q.en
        }

        holder.tvQuestion.text = langData.question.ifEmpty { q.en.question }

        val options = langData.options.ifEmpty { q.en.options }
        holder.tvOptA.text = "A) ${options.getOrElse(0) { "" }}"
        holder.tvOptB.text = "B) ${options.getOrElse(1) { "" }}"
        holder.tvOptC.text = "C) ${options.getOrElse(2) { "" }}"
        holder.tvOptD.text = "D) ${options.getOrElse(3) { "" }}"

        // Highlight correct answer
        val correctBg = "#1A2D1B"
        val transparent = "#00000000"
        holder.tvOptA.setBackgroundColor(android.graphics.Color.parseColor(if (q.correct == 0) correctBg else transparent))
        holder.tvOptB.setBackgroundColor(android.graphics.Color.parseColor(if (q.correct == 1) correctBg else transparent))
        holder.tvOptC.setBackgroundColor(android.graphics.Color.parseColor(if (q.correct == 2) correctBg else transparent))
        holder.tvOptD.setBackgroundColor(android.graphics.Color.parseColor(if (q.correct == 3) correctBg else transparent))

        val explanation = langData.explanation.ifEmpty { q.en.explanation }
        holder.tvExplanation.text = if (explanation.isNotEmpty()) "💡 $explanation" else ""

        // Language buttons
        val gold = ContextCompat.getColor(holder.itemView.context, R.color.accent_gold)
        val muted = ContextCompat.getColor(holder.itemView.context, R.color.text_muted)

        holder.btnEN.setTextColor(if (lang == "en") gold else muted)
        holder.btnUR.setTextColor(if (lang == "ur") gold else muted)
        holder.btnRoman.setTextColor(if (lang == "roman") gold else muted)

        holder.btnEN.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                currentLang[questions[p]] = "en"
                notifyItemChanged(p)
            }
        }
        holder.btnUR.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                currentLang[questions[p]] = "ur"
                notifyItemChanged(p)
            }
        }
        holder.btnRoman.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                currentLang[questions[p]] = "roman"
                notifyItemChanged(p)
            }
        }

        holder.btnDelete.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                onDelete(p)
            }
        }
    }

    override fun getItemCount() = questions.size
}