package com.quiznova.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityAddEditQuestionBinding
import com.quiznova.admin.firebase.CategoryHelper

class AddEditQuestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditQuestionBinding
    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = ""
    private var selectedDifficulty = "easy"
    private var correctAnswer = 0
    private var isEditMode = false
    private var editDocId = ""
    private var editCategory = ""

    private var categoryNames = mutableListOf<String>()
    private val difficulties = arrayOf("easy", "medium", "hard")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isEditMode = intent.hasExtra("questionId")

        binding.includeHeader.tvTitle.text = if (isEditMode) getString(R.string.title_edit_question) else getString(R.string.title_add_question)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        if (isEditMode) {
            editDocId = intent.getStringExtra("questionId") ?: ""
            editCategory = intent.getStringExtra("category") ?: ""
            binding.btnDelete.visibility = View.VISIBLE
        }

        loadCategoriesThenSetup()
        setupCorrectAnswer()
        setupButtons()
    }

    private fun loadCategoriesThenSetup() {
        binding.tvCat.text = getString(R.string.status_loading)

        CategoryHelper.clearCache()
        CategoryHelper.getCategoryNames { names ->
            runOnUiThread {
                categoryNames.clear()
                categoryNames.addAll(names)

                if (categoryNames.isNotEmpty()) {
                    selectedCategory = if (isEditMode && editCategory.isNotEmpty()) {
                        editCategory
                    } else {
                        categoryNames[0]
                    }
                    binding.tvCat.text = selectedCategory
                } else {
                    selectedCategory = getString(R.string.default_category)
                    binding.tvCat.text = selectedCategory
                }

                setupCategoryDiff()

                if (isEditMode) {
                    loadQuestion()
                }
            }
        }
    }

    private fun setupCategoryDiff() {
        binding.tvCat.setOnClickListener {
            if (categoryNames.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_no_categories), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.title_select_category))
                .setItems(categoryNames.toTypedArray()) { _, which ->
                    selectedCategory = categoryNames[which]
                    binding.tvCat.text = selectedCategory
                }
                .show()
        }

        binding.tvDiff.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.label_difficulty))
                .setItems(difficulties.map { it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()) { _, which ->
                    selectedDifficulty = difficulties[which]
                    binding.tvDiff.text = selectedDifficulty.replaceFirstChar { it.uppercase() }
                    val color = when (selectedDifficulty) {
                        "easy" -> ContextCompat.getColor(this, R.color.diff_easy)
                        "medium" -> ContextCompat.getColor(this, R.color.diff_medium)
                        "hard" -> ContextCompat.getColor(this, R.color.diff_hard)
                        else -> ContextCompat.getColor(this, R.color.accent_gold)
                    }
                    binding.tvDiff.setTextColor(color)
                }
                .show()
        }
    }

    private fun setupCorrectAnswer() {
        val buttons = listOf(binding.correctA, binding.correctB, binding.correctC, binding.correctD)

        fun updateUI() {
            buttons.forEachIndexed { i, btn ->
                if (i == correctAnswer) {
                    btn.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
                    btn.setBackgroundColor(0xFF1A3A2A.toInt())
                } else {
                    btn.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                    btn.setBackgroundColor(0xFF1A2A3A.toInt())
                }
            }
        }

        buttons.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                correctAnswer = i
                updateUI()
            }
        }
        updateUI()
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveQuestion() }
        binding.btnDelete.setOnClickListener { deleteQuestion() }
    }

    private fun loadQuestion() {
        db.collection("questions")
            .document(editCategory)
            .collection("items")
            .document(editDocId)
            .get()
            .addOnSuccessListener { doc ->
                val data = doc.data ?: return@addOnSuccessListener

                selectedCategory = data["category"] as? String ?: editCategory
                selectedDifficulty = data["difficulty"] as? String ?: "easy"
                correctAnswer = (data["correct"] as? Long)?.toInt() ?: 0

                binding.tvCat.text = selectedCategory
                binding.tvDiff.text = selectedDifficulty.replaceFirstChar { it.uppercase() }

                @Suppress("UNCHECKED_CAST")
                val en = data["en"] as? Map<String, Any> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val ur = data["ur"] as? Map<String, Any> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val roman = data["roman"] as? Map<String, Any> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val ar = data["ar"] as? Map<String, Any> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val tr = data["tr"] as? Map<String, Any> ?: emptyMap()

                binding.etQuestionEN.setText(en["question"] as? String ?: "")
                binding.etExplanationEN.setText(en["explanation"] as? String ?: "")
                val enOpts = en["options"] as? List<*> ?: listOf<Any>()
                if (enOpts.size >= 4) {
                    binding.etOptAEN.setText(enOpts[0]?.toString() ?: "")
                    binding.etOptBEN.setText(enOpts[1]?.toString() ?: "")
                    binding.etOptCEN.setText(enOpts[2]?.toString() ?: "")
                    binding.etOptDEN.setText(enOpts[3]?.toString() ?: "")
                }

                binding.etQuestionUR.setText(ur["question"] as? String ?: "")
                binding.etExplanationUR.setText(ur["explanation"] as? String ?: "")
                val urOpts = ur["options"] as? List<*> ?: listOf<Any>()
                if (urOpts.size >= 4) {
                    binding.etOptAUR.setText(urOpts[0]?.toString() ?: "")
                    binding.etOptBUR.setText(urOpts[1]?.toString() ?: "")
                    binding.etOptCUR.setText(urOpts[2]?.toString() ?: "")
                    binding.etOptDUR.setText(urOpts[3]?.toString() ?: "")
                }

                binding.etQuestionRoman.setText(roman["question"] as? String ?: "")
                binding.etExplanationRoman.setText(roman["explanation"] as? String ?: "")
                val romanOpts = roman["options"] as? List<*> ?: listOf<Any>()
                if (romanOpts.size >= 4) {
                    binding.etOptARoman.setText(romanOpts[0]?.toString() ?: "")
                    binding.etOptBRoman.setText(romanOpts[1]?.toString() ?: "")
                    binding.etOptCRoman.setText(romanOpts[2]?.toString() ?: "")
                    binding.etOptDRoman.setText(romanOpts[3]?.toString() ?: "")
                }

                binding.etQuestionAR.setText(ar["question"] as? String ?: "")
                binding.etExplanationAR.setText(ar["explanation"] as? String ?: "")

                binding.etQuestionTR.setText(tr["question"] as? String ?: "")
                binding.etExplanationTR.setText(tr["explanation"] as? String ?: "")

                val type = data["type"] as? String ?: data["mediaType"] as? String ?: "text"
                val mediaUrl = data["mediaUrl"] as? String ?: ""

                binding.etImageUrl.setText(data["imageUrl"] as? String ?: if(type == "image") mediaUrl else "")
                binding.etAudioUrl.setText(data["audioUrl"] as? String ?: if(type == "audio") mediaUrl else "")
                binding.etVideoUrl.setText(data["videoUrl"] as? String ?: if(type == "video") mediaUrl else "")

                when (type) {
                    "image" -> binding.rbImage.isChecked = true
                    "audio" -> binding.rbAudio.isChecked = true
                    "video" -> binding.rbVideo.isChecked = true
                    else -> binding.rbText.isChecked = true
                }

                setupCorrectAnswer()
            }
    }

    private fun saveQuestion() {
        val qEN = binding.etQuestionEN.text.toString().trim()
        if (qEN.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_en_q_required), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.status_saving)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: getString(R.string.label_admin_default)
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val audioUrl = binding.etAudioUrl.text.toString().trim()
        val videoUrl = binding.etVideoUrl.text.toString().trim()

        val type = when (binding.rgQuizType.checkedRadioButtonId) {
            R.id.rbImage -> "image"
            R.id.rbAudio -> "audio"
            R.id.rbVideo -> "video"
            else -> "text"
        }

        val mediaUrl = when (type) {
            "image" -> imageUrl
            "audio" -> audioUrl
            "video" -> videoUrl
            else -> ""
        }

        val data = hashMapOf(
            "en" to hashMapOf(
                "question" to binding.etQuestionEN.text.toString().trim(),
                "options" to listOf(
                    binding.etOptAEN.text.toString().trim(),
                    binding.etOptBEN.text.toString().trim(),
                    binding.etOptCEN.text.toString().trim(),
                    binding.etOptDEN.text.toString().trim()
                ),
                "explanation" to binding.etExplanationEN.text.toString().trim()
            ),
            "ur" to hashMapOf(
                "question" to binding.etQuestionUR.text.toString().trim(),
                "options" to listOf(
                    binding.etOptAUR.text.toString().trim(),
                    binding.etOptBUR.text.toString().trim(),
                    binding.etOptCUR.text.toString().trim(),
                    binding.etOptDUR.text.toString().trim()
                ),
                "explanation" to binding.etExplanationUR.text.toString().trim()
            ),
            "roman" to hashMapOf(
                "question" to binding.etQuestionRoman.text.toString().trim(),
                "options" to listOf(
                    binding.etOptARoman.text.toString().trim(),
                    binding.etOptBRoman.text.toString().trim(),
                    binding.etOptCRoman.text.toString().trim(),
                    binding.etOptDRoman.text.toString().trim()
                ),
                "explanation" to binding.etExplanationRoman.text.toString().trim()
            ),
            "ar" to hashMapOf(
                "question" to binding.etQuestionAR.text.toString().trim(),
                "explanation" to binding.etExplanationAR.text.toString().trim()
            ),
            "tr" to hashMapOf(
                "question" to binding.etQuestionTR.text.toString().trim(),
                "explanation" to binding.etExplanationTR.text.toString().trim()
            ),
            "correct" to correctAnswer,
            "difficulty" to selectedDifficulty,
            "category" to selectedCategory,
            "type" to type,
            "mediaType" to type,
            "mediaUrl" to mediaUrl,
            "tags" to listOf<String>(),
            "hasImage" to imageUrl.isNotEmpty(),
            "imageUrl" to imageUrl,
            "audioUrl" to audioUrl,
            "videoUrl" to videoUrl,
            "isActive" to true,
            "createdAt" to Timestamp.now(),
            "createdBy" to email
        )

        if (isEditMode) {
            db.collection("questions")
                .document(editCategory)
                .collection("items")
                .document(editDocId)
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_updated), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.btn_save_question)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            db.collection("questions")
                .document(selectedCategory)
                .collection("items")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.msg_added), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.btn_save_question)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun deleteQuestion() {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle(getString(R.string.title_delete_q_confirm))
            .setMessage(getString(R.string.msg_delete_confirm))
            .setPositiveButton(getString(R.string.btn_delete_confirm)) { _, _ ->
                db.collection("questions")
                    .document(editCategory)
                    .collection("items")
                    .document(editDocId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.msg_deleted), Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}