package com.quiznova.admin

import android.content.Intent
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
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityManageQuestionsBinding
import com.quiznova.admin.firebase.CategoryHelper
import com.quiznova.admin.models.QuestionData
import com.quiznova.admin.models.QuestionLang

class ManageQuestionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageQuestionsBinding
    private val db = FirebaseFirestore.getInstance()
    private val allQuestions = mutableListOf<QuestionData>()
    private val displayedQuestions = mutableListOf<QuestionData>()
    private lateinit var adapter: QuestionListAdapter

    private var filterCategory = "All"
    private var filterDifficulty = "All"
    private var filterType = "All"
    private var categoryNames = listOf<String>()
    private val difficulties = arrayOf("All", "easy", "medium", "hard")
    private val types = arrayOf("All", "text", "image", "audio", "video")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = "Manage Questions"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = QuestionListAdapter(displayedQuestions,
            onEdit = { q ->
                val intent = Intent(this, AddEditQuestionActivity::class.java)
                intent.putExtra("questionId", q.id)
                intent.putExtra("category", q.category)
                startActivity(intent)
            },
            onDelete = { q -> deleteQuestion(q) }
        )

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter

        binding.tvFilterCategory.setOnClickListener { showCategoryFilter() }
        binding.tvFilterDifficulty.setOnClickListener { showDifficultyFilter() }
        binding.tvFilterType.setOnClickListener { showTypeFilter() }
        binding.btnAddNew.setOnClickListener {
            startActivity(Intent(this, AddEditQuestionActivity::class.java))
        }

        // ═══ Delete All Button ═══
        binding.btnDeleteAll.setOnClickListener { confirmDeleteAll() }

        loadCategoriesThenQuestions()
    }

    override fun onResume() {
        super.onResume()
        loadCategoriesThenQuestions()
    }

    // ════════════════════════════════════════════
    //  LOAD CATEGORIES FIRST, THEN QUESTIONS
    // ════════════════════════════════════════════

    private fun loadCategoriesThenQuestions() {
        CategoryHelper.clearCache()

        CategoryHelper.getCategoryNames { names ->
            runOnUiThread {
                categoryNames = names
                loadQuestions()
            }
        }
    }

    private fun loadQuestions() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        allQuestions.clear()

        if (categoryNames.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvCount.text = "0 questions (no categories)"
            return
        }

        var loaded = 0
        val totalCats = categoryNames.size

        for (cat in categoryNames) {
            db.collection("questions")
                .document(cat)
                .collection("items")
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val en = parseLang(data["en"] as? Map<*, *>)
                        val ur = parseLang(data["ur"] as? Map<*, *>)
                        val roman = parseLang(data["roman"] as? Map<*, *>)

                        allQuestions.add(
                            QuestionData(
                                id = doc.id,
                                en = en,
                                ur = ur,
                                roman = roman,
                                correct = (data["correct"] as? Long)?.toInt() ?: 0,
                                difficulty = data["difficulty"] as? String ?: "easy",
                                category = data["category"] as? String ?: cat,
                                type = data["type"] as? String ?: "text",
                                isActive = data["isActive"] as? Boolean ?: true
                            )
                        )
                    }
                    loaded++
                    if (loaded >= totalCats) {
                        binding.progressBar.visibility = View.GONE
                        applyFilter()
                    }
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded >= totalCats) {
                        binding.progressBar.visibility = View.GONE
                        applyFilter()
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLang(map: Map<*, *>?): QuestionLang {
        if (map == null) return QuestionLang()
        return QuestionLang(
            question = map["question"] as? String ?: "",
            options = (map["options"] as? List<String>) ?: listOf("", "", "", ""),
            explanation = map["explanation"] as? String ?: ""
        )
    }

    // ════════════════════════════════════════════
    //  FILTERS
    // ════════════════════════════════════════════

    private fun showCategoryFilter() {
        val options = mutableListOf("All Categories")
        options.addAll(categoryNames)

        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Filter by Category")
            .setItems(options.toTypedArray()) { _, which ->
                filterCategory = if (which == 0) "All" else options[which]
                binding.tvFilterCategory.text = filterCategory
                applyFilter()
            }
            .show()
    }

    private fun showDifficultyFilter() {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Filter by Difficulty")
            .setItems(difficulties.map { if (it == "All") "All Levels" else it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()) { _, which ->
                filterDifficulty = difficulties[which]
                binding.tvFilterDifficulty.text = if (filterDifficulty == "All") "All Levels" else filterDifficulty.replaceFirstChar { it.uppercase() }
                applyFilter()
            }
            .show()
    }

    private fun showTypeFilter() {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Filter by Type")
            .setItems(types.map { if (it == "All") "All Types" else it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()) { _, which ->
                filterType = types[which]
                binding.tvFilterType.text = if (filterType == "All") "All Types" else filterType.replaceFirstChar { it.uppercase() }
                applyFilter()
            }
            .show()
    }

    private fun applyFilter() {
        displayedQuestions.clear()
        displayedQuestions.addAll(allQuestions.filter { q ->
            (filterCategory == "All" || q.category == filterCategory) &&
                    (filterDifficulty == "All" || q.difficulty == filterDifficulty) &&
                    (filterType == "All" || q.type == filterType)
        })
        adapter.notifyDataSetChanged()
        binding.tvCount.text = "${displayedQuestions.size} questions"
        binding.tvEmpty.visibility = if (displayedQuestions.isEmpty()) View.VISIBLE else View.GONE
    }

    // ════════════════════════════════════════════
    //  DELETE ALL (FILTERED)
    // ════════════════════════════════════════════

    private fun confirmDeleteAll() {
        if (displayedQuestions.isEmpty()) {
            Toast.makeText(this, "No questions to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryLabel = if (filterCategory == "All") "All Categories" else filterCategory
        val difficultyLabel = if (filterDifficulty == "All") "All Levels" else filterDifficulty.replaceFirstChar { it.uppercase() }

        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Delete All Questions?")
            .setMessage(
                "You are about to delete ${displayedQuestions.size} questions.\n\n" +
                        "Category: $categoryLabel\n" +
                        "Difficulty: $difficultyLabel\n\n" +
                        "This action cannot be undone!"
            )
            .setPositiveButton("DELETE ALL") { _, _ -> executeDeleteAll() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeDeleteAll() {
        binding.progressBar.visibility = View.VISIBLE
        val toDelete = displayedQuestions.toList() // copy so we don't modify while iterating

        // Group by category for efficient batch writes
        // Firestore batch limit is 500 — if more, split into multiple batches
        val batches = mutableListOf<com.google.firebase.firestore.WriteBatch>()
        var currentBatch = db.batch()
        var opCount = 0

        for (q in toDelete) {
            if (opCount >= 500) {
                batches.add(currentBatch)
                currentBatch = db.batch()
                opCount = 0
            }
            val ref = db.collection("questions")
                .document(q.category)
                .collection("items")
                .document(q.id)
            currentBatch.delete(ref)
            opCount++
        }
        if (opCount > 0) {
            batches.add(currentBatch)
        }

        var completed = 0
        val totalBatches = batches.size
        var hasError = false

        for (batch in batches) {
            batch.commit()
                .addOnSuccessListener {
                    completed++
                    if (completed >= totalBatches && !hasError) {
                        onDeleteAllSuccess(toDelete)
                    }
                }
                .addOnFailureListener { e ->
                    completed++
                    hasError = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun onDeleteAllSuccess(deleted: List<QuestionData>) {
        val deletedIds = deleted.map { it.id }.toSet()
        allQuestions.removeAll { it.id in deletedIds }
        applyFilter()
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this, "${deleted.size} questions deleted", Toast.LENGTH_SHORT).show()
    }

    // ════════════════════════════════════════════
    //  DELETE SINGLE
    // ════════════════════════════════════════════

    private fun deleteQuestion(q: QuestionData) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Delete Question?")
            .setMessage(q.en.question.take(80))
            .setPositiveButton("Delete") { _, _ ->
                db.collection("questions")
                    .document(q.category)
                    .collection("items")
                    .document(q.id)
                    .delete()
                    .addOnSuccessListener {
                        allQuestions.removeAll { it.id == q.id }
                        applyFilter()
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ════════════════════════════════════════════
//  LIST ADAPTER
// ════════════════════════════════════════════

class QuestionListAdapter(
    private val questions: List<QuestionData>,
    private val onEdit: (QuestionData) -> Unit,
    private val onDelete: (QuestionData) -> Unit
) : RecyclerView.Adapter<QuestionListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestionText)
        val tvCategory: TextView = view.findViewById(R.id.tvQuestionCategory)
        val tvDifficulty: TextView = view.findViewById(R.id.tvQuestionDifficulty)
        val tvType: TextView = view.findViewById(R.id.tvQuestionType)
        val btnEdit: TextView = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteQ)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_list, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = questions[position]
        holder.tvQuestion.text = q.en.question.ifEmpty { "No question text" }
        holder.tvCategory.text = q.category
        holder.tvDifficulty.text = q.difficulty.replaceFirstChar { it.uppercase() }

        val diffColor = when (q.difficulty) {
            "easy" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_easy)
            "medium" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_medium)
            "hard" -> ContextCompat.getColor(holder.itemView.context, R.color.diff_hard)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.accent_gold)
        }
        holder.tvDifficulty.setTextColor(diffColor)

        val typeRes = when (q.type) {
            "image" -> R.string.type_image_caps
            "audio" -> R.string.type_audio_caps
            "video" -> R.string.type_video_caps
            else -> R.string.type_text_caps
        }
        holder.tvType.setText(typeRes)

        holder.btnEdit.setOnClickListener { onEdit(q) }
        holder.btnDelete.setOnClickListener { onDelete(q) }
    }

    override fun getItemCount() = questions.size
}