package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.ai.AiClient
import com.quiznova.admin.ai.PromptBuilder
import com.quiznova.admin.ai.ResponseParser
import com.quiznova.admin.databinding.ActivityGenerateBinding
import com.quiznova.admin.firebase.CategoryHelper
import com.quiznova.admin.models.QuestionData
import kotlinx.coroutines.launch

class GenerateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerateBinding
    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = ""
    private var selectedDifficulty = "easy"
    private var selectedType = "text"
    private var questionCount = 10
    private var selectedLanguageIndex = 2

    private var categoryNames = mutableListOf<String>()

    private val difficulties = arrayOf("easy", "medium", "hard")
    private val quizTypes = arrayOf("text", "image", "audio", "video")
    private val languageLabels = arrayOf("EN + UR + Roman", "EN + UR", "English Only")
    private val languageCodes = arrayOf("en_ur_roman", "en_ur", "en")

    private var generatedQuestions = listOf<QuestionData>()
    private var apiKey = ""
    private var aiProvider = "groq"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        loadApiConfig()
        loadCategories()
        setupSelectors()
        setupCounter()
        setupButtons()

        binding.tvType.text = selectedType.replaceFirstChar { it.uppercase() }
    }

    private fun setupHeader() {
        binding.includeHeader.tvTitle.text = getString(R.string.title_generate)
        binding.includeHeader.btnBack.setOnClickListener { finish() }
    }

    private fun loadApiConfig() {
        db.collection("admin").document("config").get()
            .addOnSuccessListener { doc ->
                apiKey = doc.getString("aiApiKey") ?: ""
                aiProvider = doc.getString("aiProvider") ?: "groq"

                if (apiKey.isEmpty()) {
                    showError(getString(R.string.error_no_api_key))
                    binding.btnGenerate.isEnabled = false
                    binding.btnGenerate.text = getString(R.string.status_api_key_required)
                }
            }
    }

    private fun loadCategories() {
        binding.tvCategory.text = getString(R.string.status_loading)

        CategoryHelper.getCategoryNames { names ->
            runOnUiThread {
                categoryNames.clear()
                categoryNames.addAll(names)

                if (categoryNames.isNotEmpty()) {
                    selectedCategory = categoryNames[0]
                    binding.tvCategory.text = selectedCategory
                } else {
                    selectedCategory = getString(R.string.default_category)
                    binding.tvCategory.text = selectedCategory
                }
            }
        }
    }

    private fun setupSelectors() {
        binding.rowCategory.setOnClickListener {
            if (categoryNames.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_loading_categories), Toast.LENGTH_SHORT).show()
                CategoryHelper.clearCache()
                loadCategories()
                return@setOnClickListener
            }

            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.title_select_category))
                .setItems(categoryNames.toTypedArray()) { _, which ->
                    selectedCategory = categoryNames[which]
                    binding.tvCategory.text = selectedCategory
                }
                .show()
        }

        binding.rowDifficulty.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.title_select_difficulty))
                .setItems(difficulties.map { it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()) { _, which ->
                    selectedDifficulty = difficulties[which]
                    binding.tvDifficulty.text = selectedDifficulty.replaceFirstChar { it.uppercase() }
                    val color = when (selectedDifficulty) {
                        "easy" -> ContextCompat.getColor(this, R.color.diff_easy)
                        "medium" -> ContextCompat.getColor(this, R.color.diff_medium)
                        "hard" -> ContextCompat.getColor(this, R.color.diff_hard)
                        else -> ContextCompat.getColor(this, R.color.accent_gold)
                    }
                    binding.tvDifficulty.setTextColor(color)
                }
                .show()
        }

        binding.rowLanguage.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.title_select_languages))
                .setSingleChoiceItems(languageLabels, selectedLanguageIndex) { dialog, which ->
                    selectedLanguageIndex = which
                    binding.tvLanguage.text = languageLabels[which]
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupCounter() {
        binding.tvCount.text = questionCount.toString()

        binding.btnPlus.setOnClickListener {
            if (questionCount < 50) {
                questionCount++
                binding.tvCount.text = questionCount.toString()
            }
        }

        binding.btnMinus.setOnClickListener {
            if (questionCount > 1) {
                questionCount--
                binding.tvCount.text = questionCount.toString()
            }
        }
    }

    private fun setupButtons() {
        binding.btnGenerate.setOnClickListener {
            if (apiKey.isEmpty()) {
                showError(getString(R.string.error_no_api_key))
                return@setOnClickListener
            }
            if (selectedCategory.isEmpty()) {
                showError(getString(R.string.error_select_category))
                return@setOnClickListener
            }
            startGeneration()
        }

        binding.btnReview.setOnClickListener {
            ReviewActivity.questionsToReview = generatedQuestions
            startActivity(Intent(this, ReviewActivity::class.java))
        }

        binding.btnUploadDirect.setOnClickListener {
            uploadQuestions(generatedQuestions)
        }
    }

    private fun startGeneration() {
        binding.layoutProgress.isVisible = true
        binding.layoutResult.isVisible = false
        binding.btnGenerate.isEnabled = false
        hideError()

        binding.tvProgress.text = getString(R.string.format_generating_questions, questionCount)
        binding.tvProgressDetail.text = getString(R.string.format_generating_detail, aiProvider, selectedCategory, selectedDifficulty)

        val customTopic = binding.etCustomTopic.text.toString().trim()
        val langCode = languageCodes[selectedLanguageIndex]

        val prompt = PromptBuilder.buildGeneratePrompt(
            category = selectedCategory,
            difficulty = selectedDifficulty,
            count = questionCount,
            languages = langCode,
            customTopic = customTopic,
            type = selectedType
        )

        val client = AiClient(apiKey, aiProvider)

        lifecycleScope.launch {
            binding.tvProgressDetail.text = getString(R.string.status_calling_ai, aiProvider)

            val result = client.generateQuestions(prompt)

            result.onSuccess { response ->
                binding.tvProgressDetail.text = getString(R.string.status_parsing)
                try {
                    generatedQuestions = ResponseParser.parseQuestions(response)

                    binding.layoutProgress.isVisible = false
                    binding.btnGenerate.isEnabled = true

                    if (generatedQuestions.isEmpty()) {
                        showError(getString(R.string.error_zero_questions))
                    } else {
                        binding.layoutResult.isVisible = true
                        binding.tvResultCount.text = getString(R.string.format_questions_gen_success, generatedQuestions.size)
                        logAction("Generated ${generatedQuestions.size} $selectedDifficulty $selectedCategory questions via $aiProvider")
                    }
                } catch (e: Exception) {
                    binding.layoutProgress.isVisible = false
                    binding.btnGenerate.isEnabled = true
                    showError(getString(R.string.error_config_save, e.message))
                }
            }

            result.onFailure { e ->
                binding.layoutProgress.isVisible = false
                binding.btnGenerate.isEnabled = true
                showError(getString(R.string.error_config_save, e.message))
            }
        }
    }

    private fun uploadQuestions(questions: List<QuestionData>) {
        if (questions.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_questions_upload), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUploadDirect.isEnabled = false
        binding.btnUploadDirect.text = getString(R.string.status_uploading_dots)

        var uploaded = 0
        var failed = 0
        val total = questions.size
        val email = FirebaseAuth.getInstance().currentUser?.email ?: getString(R.string.label_admin_default)

        for (q in questions) {
            val catName = q.category.ifEmpty { selectedCategory }

            val data = hashMapOf(
                "en" to hashMapOf(
                    "question" to q.en.question,
                    "options" to q.en.options,
                    "explanation" to q.en.explanation
                ),
                "ur" to hashMapOf(
                    "question" to q.ur.question,
                    "options" to q.ur.options,
                    "explanation" to q.ur.explanation
                ),
                "roman" to hashMapOf(
                    "question" to q.roman.question,
                    "options" to q.roman.options,
                    "explanation" to q.roman.explanation
                ),
                "ar" to hashMapOf(
                    "question" to "",
                    "explanation" to ""
                ),
                "tr" to hashMapOf(
                    "question" to "",
                    "explanation" to ""
                ),
                "correct" to q.correct,
                "difficulty" to q.difficulty,
                "category" to catName,
                "type" to q.type.ifEmpty { selectedType },
                "mediaType" to q.type.ifEmpty { selectedType },
                "mediaUrl" to q.imageUrl,
                "tags" to q.tags,
                "hasImage" to q.imageUrl.isNotEmpty(),
                "imageUrl" to q.imageUrl,
                "audioUrl" to if (q.type == "audio") q.imageUrl else "",
                "videoUrl" to if (q.type == "video") q.imageUrl else "",
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
                    if (uploaded + failed >= total) onUploadComplete(uploaded, failed)
                }
                .addOnFailureListener {
                    failed++
                    if (uploaded + failed >= total) onUploadComplete(uploaded, failed)
                }
        }
    }

    private fun onUploadComplete(uploaded: Int, failed: Int) {
        binding.btnUploadDirect.isEnabled = true
        binding.btnUploadDirect.text = getString(R.string.btn_upload_directly)

        if (failed == 0) {
            Toast.makeText(this, getString(R.string.msg_all_uploaded, uploaded), Toast.LENGTH_LONG).show()
            logAction("Uploaded $uploaded questions to Firebase")
            finish()
        } else {
            Toast.makeText(this, getString(R.string.format_upload_stats, uploaded, failed), Toast.LENGTH_LONG).show()
        }
    }

    private fun logAction(details: String) {
        val log = hashMapOf(
            "action" to "generate_questions",
            "details" to details,
            "adminEmail" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "timestamp" to Timestamp.now()
        )
        db.collection("admin").document("logs").collection("entries").add(log)
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.isVisible = true
    }

    private fun hideError() {
        binding.tvError.isVisible = false
    }
}