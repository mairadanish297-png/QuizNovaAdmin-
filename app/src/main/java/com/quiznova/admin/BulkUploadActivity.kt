package com.quiznova.admin

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.quiznova.admin.databinding.ActivityBulkUploadBinding
import com.quiznova.admin.firebase.CategoryHelper

class BulkUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkUploadBinding
    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = "General Knowledge"
    private val categoryNames = mutableListOf<String>()
    private var currentTab = "json"
    private val parsedQuestions = mutableListOf<Map<String, Any>>()
    private var csvContent = ""
    private lateinit var previewAdapter: QuestionPreviewAdapter

    companion object {
        private const val TAG = "BulkUpload"
    }

    private val csvPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    var rawContent = inputStream?.bufferedReader().use { it?.readText() ?: "" }
                    inputStream?.close()

                    if (rawContent.startsWith("\uFEFF")) {
                        rawContent = rawContent.substring(1)
                    }
                    csvContent = rawContent.trim()

                    val fileName = uri.lastPathSegment ?: "file.csv"
                    binding.tvCsvFileName.text = fileName
                    binding.etCsvPreview.setText(csvContent.take(2000))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBulkUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.label_bulk_upload)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        setupPreviewRecyclerView()
        loadCategories()
        setupTabs()
        setupButtons()
        
        binding.rbText.isChecked = true
    }

    private fun setupPreviewRecyclerView() {
        previewAdapter = QuestionPreviewAdapter(mutableListOf()) { position ->
            deleteQuestion(position)
        }
        binding.rvPreview.layoutManager = LinearLayoutManager(this)
        binding.rvPreview.adapter = previewAdapter
    }

    private fun loadCategories() {
        CategoryHelper.clearCache()
        CategoryHelper.getCategoryNames { names ->
            runOnUiThread {
                categoryNames.clear()
                categoryNames.addAll(names)
                Log.d(TAG, "Loaded ${names.size} categories: $names")
                if (categoryNames.isNotEmpty()) {
                    selectedCategory = categoryNames[0]
                    binding.tvCategory.text = selectedCategory
                }
            }
        }
    }

    private fun setupTabs() {
        val gold = ContextCompat.getColor(this, R.color.accent_gold)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        fun updateTabs() {
            binding.tabJson.setTextColor(if (currentTab == "json") gold else muted)
            binding.tabCsv.setTextColor(if (currentTab == "csv") gold else muted)
            binding.layoutJsonSection.isVisible = currentTab == "json"
            binding.layoutCsvSection.isVisible = currentTab == "csv"
        }

        binding.tabJson.setOnClickListener {
            currentTab = "json"
            updateTabs()
        }

        binding.tabCsv.setOnClickListener {
            currentTab = "csv"
            updateTabs()
        }
    }

    private fun setupButtons() {
        binding.rowCategory.setOnClickListener {
            if (categoryNames.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_no_categories), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
                .setTitle(getString(R.string.title_select_default_cat))
                .setItems(categoryNames.toTypedArray()) { _, which ->
                    selectedCategory = categoryNames[which]
                    binding.tvCategory.text = selectedCategory
                }
                .show()
        }

        binding.btnShowTemplate.setOnClickListener { showJsonTemplate() }
        binding.btnDownloadTemplate.setOnClickListener { downloadCsvTemplate() }
        binding.btnSelectCsv.setOnClickListener { selectCsvFile() }
        binding.btnValidate.setOnClickListener { validateInput() }
        binding.btnUpload.setOnClickListener { uploadQuestions() }
    }

    private fun showJsonTemplate() {
        val template = """[
  {
    "en": {
      "question": "What is the capital of Pakistan?",
      "options": ["Karachi", "Lahore", "Islamabad", "Peshawar"],
      "explanation": "Islamabad has been the capital since 1960."
    },
    "type": "text",
    "imageUrl": "",
    "audioUrl": "",
    "videoUrl": "",
    "correct": 2,
    "difficulty": "easy",
    "category": "General Knowledge"
  }
]"""
        binding.etJsonInput.setText(template)
        Toast.makeText(this, getString(R.string.msg_template_loaded), Toast.LENGTH_SHORT).show()
    }

    private fun downloadCsvTemplate() {
        val template = """question_en,options_en,explanation_en,correct,difficulty,category,type,imageUrl,audioUrl,videoUrl
"What is the capital of Pakistan?","Karachi|Lahore|Islamabad|Peshawar","Islamabad has been the capital since 1960",2,easy,General Knowledge,text,,,"""

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.label_csv_template_clip), template)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, getString(R.string.msg_csv_copied), Toast.LENGTH_LONG).show()
        binding.etCsvPreview.setText(template)
    }

    private fun selectCsvFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        csvPicker.launch(Intent.createChooser(intent, getString(R.string.title_select_csv)))
    }

    // ========== VALIDATE ==========

    private fun validateInput() {
        val selectedType = when (binding.rgQuizType.checkedRadioButtonId) {
            R.id.rbImage -> "image"
            R.id.rbAudio -> "audio"
            R.id.rbVideo -> "video"
            else -> "text"
        }

        parsedQuestions.clear()
        val errors = mutableListOf<String>()

        if (currentTab == "json") {
            validateJson(errors, selectedType)
        } else {
            validateCsv(errors, selectedType)
        }

        binding.layoutValidation.isVisible = true

        if (errors.isEmpty()) {
            binding.tvValidationResult.text = getString(R.string.format_valid_questions, parsedQuestions.size)
            binding.tvValidationResult.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            binding.tvValidationErrors.isVisible = false
            showPreview()
        } else {
            binding.tvValidationResult.text = getString(R.string.format_validation_summary, parsedQuestions.size, errors.size)
            binding.tvValidationResult.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
            binding.tvValidationErrors.text = errors.joinToString("\n")
            binding.tvValidationErrors.isVisible = true

            if (parsedQuestions.isNotEmpty()) {
                showPreview()
            } else {
                hidePreview()
            }
        }
    }

    // ========== VALIDATE JSON ==========

    private fun validateJson(errors: MutableList<String>, overrideType: String) {
        val jsonStr = binding.etJsonInput.text.toString().trim()

        if (jsonStr.isEmpty()) {
            errors.add(getString(R.string.msg_json_empty))
            return
        }

        try {
            val parsedElement = JsonParser.parseString(jsonStr)
            val jsonArray = when {
                parsedElement.isJsonArray -> parsedElement.asJsonArray
                parsedElement.isJsonObject -> com.google.gson.JsonArray().apply { add(parsedElement.asJsonObject) }
                else -> {
                    errors.add("Invalid JSON: Root element must be an array or object")
                    return
                }
            }

            for (i in 0 until jsonArray.size()) {
                try {
                    val obj = jsonArray[i].asJsonObject
                    val en = obj.getAsJsonObject("en")
                    val question = en?.get("question")?.asString ?: ""
                    val options = en?.getAsJsonArray("options")

                    if (question.isEmpty()) {
                        errors.add(getString(R.string.format_row_error, i + 1, "Missing English question"))
                        continue
                    }

                    if (options == null || options.size() < 2) {
                        errors.add(getString(R.string.format_row_error, i + 1, "Need at least 2 options"))
                        continue
                    }

                    val correct = obj.get("correct")?.asInt ?: 0
                    if (correct < 0 || correct >= options.size()) {
                        errors.add(getString(R.string.format_row_error, i + 1, "Correct answer index out of range"))
                        continue
                    }

                    val difficulty = obj.get("difficulty")?.asString ?: "easy"
                    val category = obj.get("category")?.asString ?: selectedCategory
                    val type = obj.get("type")?.asString ?: overrideType

                    val questionData = buildQuestionMap(obj, category, difficulty, type)
                    parsedQuestions.add(questionData)

                } catch (e: Exception) {
                    errors.add(getString(R.string.format_row_error, i + 1, "Parse error - ${e.message?.take(50)}"))
                }
            }

        } catch (e: Exception) {
            errors.add("Invalid JSON format: ${e.message?.take(100)}")
        }
    }

    // ========== VALIDATE CSV ==========

    private fun validateCsv(errors: MutableList<String>, overrideType: String) {
        val content = if (csvContent.isNotEmpty()) csvContent else binding.etCsvPreview.text.toString().trim()

        if (content.isEmpty()) {
            errors.add("CSV is empty. Paste or upload a CSV file.")
            return
        }

        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size < 2) {
            errors.add("CSV needs at least a header row and one data row.")
            return
        }

        val header = parseCsvLine(lines[0]).map { it.trim().lowercase() }

        val debugCols = header.joinToString(", ")
        Log.d(TAG, "CSV Headers: $debugCols")

        val questionEnIdx = header.indexOfFirst {
            it.contains("question_en") || it.contains("questionen") || it == "question"
        }
        val optionsEnIdx = header.indexOfFirst {
            it.contains("options_en") || it.contains("optionsen") || it == "options"
        }
        val explanationEnIdx = header.indexOfFirst {
            it.contains("explanation_en") || it.contains("explanationen") || it == "explanation"
        }
        val questionUrIdx = header.indexOfFirst {
            it.contains("question_ur") || it.contains("questionur")
        }
        val optionsUrIdx = header.indexOfFirst {
            it.contains("options_ur") || it.contains("optionsur")
        }
        val explanationUrIdx = header.indexOfFirst {
            it.contains("explanation_ur") || it.contains("explanationur")
        }
        val questionRomanIdx = header.indexOfFirst {
            it.contains("question_roman") || it.contains("questionroman")
        }
        val optionsRomanIdx = header.indexOfFirst {
            it.contains("options_roman") || it.contains("optionsroman")
        }
        val explanationRomanIdx = header.indexOfFirst {
            it.contains("explanation_roman") || it.contains("explanationroman")
        }
        val correctIdx = header.indexOfFirst { it.contains("correct") }
        val difficultyIdx = header.indexOfFirst { it.contains("difficulty") }
        val categoryIdx = header.indexOfFirst { it.contains("category") }
        val typeIdx = header.indexOfFirst { it == "type" }
        val imageIdx = header.indexOfFirst { it.contains("image") }
        val audioIdx = header.indexOfFirst { it.contains("audio") }
        val videoIdx = header.indexOfFirst { it.contains("video") }

        if (questionEnIdx < 0 || optionsEnIdx < 0) {
            errors.add("CSV must have 'question_en' and 'options_en' columns! Found: $debugCols")
            return
        }

        for (i in 1 until lines.size) {
            val rowNum = i + 1
            try {
                val cols = parseCsvLine(lines[i])

                if (cols.size <= questionEnIdx) {
                    errors.add("Row $rowNum: Not enough columns (need at least ${questionEnIdx + 1})")
                    continue
                }

                val questionEn = cols[questionEnIdx].trim()
                val optionsEnRaw = cols.getOrNull(optionsEnIdx)?.trim() ?: ""

                if (questionEn.isEmpty()) {
                    errors.add("Row $rowNum: Empty question")
                    continue
                }

                val optionsEn = optionsEnRaw.split("|").map { it.trim() }
                if (optionsEn.size < 2) {
                    errors.add("Row $rowNum: Need at least 2 options (use | separator)")
                    continue
                }

                val explanationEn = cols.getOrNull(explanationEnIdx)?.trim() ?: ""
                val questionUr = cols.getOrNull(questionUrIdx)?.trim() ?: ""
                val optionsUrRaw = cols.getOrNull(optionsUrIdx)?.trim() ?: ""
                val explanationUr = cols.getOrNull(explanationUrIdx)?.trim() ?: ""
                val questionRoman = cols.getOrNull(questionRomanIdx)?.trim() ?: ""
                val optionsRomanRaw = cols.getOrNull(optionsRomanIdx)?.trim() ?: ""
                val explanationRoman = cols.getOrNull(explanationRomanIdx)?.trim() ?: ""
                val correct = cols.getOrNull(correctIdx)?.trim()?.toIntOrNull() ?: 0
                val difficulty = cols.getOrNull(difficultyIdx)?.trim()?.ifEmpty { "easy" } ?: "easy"
                val category = cols.getOrNull(categoryIdx)?.trim()?.ifEmpty { selectedCategory } ?: selectedCategory
                val type = cols.getOrNull(typeIdx)?.trim()?.ifEmpty { overrideType } ?: overrideType
                val imageUrl = cols.getOrNull(imageIdx)?.trim() ?: ""
                val audioUrl = cols.getOrNull(audioIdx)?.trim() ?: ""
                val videoUrl = cols.getOrNull(videoIdx)?.trim() ?: ""

                val optionsUr = if (optionsUrRaw.isNotEmpty()) optionsUrRaw.split("|").map { it.trim() } else optionsEn
                val optionsRoman = if (optionsRomanRaw.isNotEmpty()) optionsRomanRaw.split("|").map { it.trim() } else optionsEn

                val questionData = hashMapOf(
                    "en" to hashMapOf(
                        "question" to questionEn,
                        "options" to optionsEn,
                        "explanation" to explanationEn
                    ),
                    "ur" to hashMapOf(
                        "question" to questionUr.ifEmpty { questionEn },
                        "options" to optionsUr,
                        "explanation" to explanationUr
                    ),
                    "roman" to hashMapOf(
                        "question" to questionRoman.ifEmpty { questionEn },
                        "options" to optionsRoman,
                        "explanation" to explanationRoman
                    ),
                    "correct" to correct.coerceIn(0, optionsEn.size - 1),
                    "difficulty" to difficulty,
                    "category" to category,
                    "type" to type,
                    "mediaType" to type,
                    "mediaUrl" to when (type) {
                        "image" -> imageUrl
                        "audio" -> audioUrl
                        "video" -> videoUrl
                        else -> ""
                    },
                    "tags" to emptyList<String>(),
                    "hasImage" to imageUrl.isNotEmpty(),
                    "imageUrl" to imageUrl,
                    "audioUrl" to audioUrl,
                    "videoUrl" to videoUrl,
                    "isActive" to true,
                    "timesAnswered" to 0,
                    "timesCorrect" to 0,
                    "createdAt" to Timestamp.now(),
                    "createdBy" to (FirebaseAuth.getInstance().currentUser?.email ?: "admin")
                )

                parsedQuestions.add(questionData)

            } catch (e: Exception) {
                errors.add("Row $rowNum: ${e.message?.take(50)}")
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when (char) {
                '\"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                } else {
                    current.append(char)
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    // ========== BUILD QUESTION MAP FROM JSON ==========

    private fun buildQuestionMap(obj: JsonObject, category: String, difficulty: String, overrideType: String): Map<String, Any> {
        fun parseLang(langObj: JsonObject?): HashMap<String, Any> {
            if (langObj == null) return hashMapOf("question" to "", "options" to emptyList<String>(), "explanation" to "")
            val question = langObj.get("question")?.asString ?: ""
            val options = try {
                langObj.getAsJsonArray("options").map { it.asString }
            } catch (_: Exception) { listOf("", "", "", "") }
            val explanation = langObj.get("explanation")?.asString ?: ""
            return hashMapOf("question" to question, "options" to options, "explanation" to explanation)
        }

        val en = parseLang(obj.getAsJsonObject("en"))
        val ur = parseLang(obj.getAsJsonObject("ur"))
        val roman = parseLang(obj.getAsJsonObject("roman"))
        val correct = obj.get("correct")?.asInt ?: 0
        val catFromJson = obj.get("category")?.asString ?: category
        val type = obj.get("type")?.asString ?: overrideType
        val imageUrl = obj.get("imageUrl")?.asString ?: ""
        val audioUrl = obj.get("audioUrl")?.asString ?: ""
        val videoUrl = obj.get("videoUrl")?.asString ?: ""

        return hashMapOf(
            "en" to en,
            "ur" to ur,
            "roman" to roman,
            "correct" to correct.coerceIn(0, 3),
            "difficulty" to difficulty,
            "category" to catFromJson.ifEmpty { category },
            "type" to type,
            "mediaType" to type,
            "mediaUrl" to when (type) {
                "image" -> imageUrl
                "audio" -> audioUrl
                "video" -> videoUrl
                else -> ""
            },
            "tags" to try { obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList() } catch (_: Exception) { emptyList() },
            "hasImage" to imageUrl.isNotEmpty(),
            "imageUrl" to imageUrl,
            "audioUrl" to audioUrl,
            "videoUrl" to videoUrl,
            "isActive" to true,
            "timesAnswered" to 0,
            "timesCorrect" to 0,
            "createdAt" to Timestamp.now(),
            "createdBy" to (FirebaseAuth.getInstance().currentUser?.email ?: "admin")
        )
    }

    private fun showPreview() {
        binding.layoutPreview.isVisible = true
        binding.tvPreviewCount.text = getString(R.string.format_ready_to_upload, parsedQuestions.size)
        previewAdapter.updateData(parsedQuestions.toList())
        binding.btnUpload.isVisible = true
    }

    private fun hidePreview() {
        binding.layoutPreview.isVisible = false
        binding.btnUpload.isVisible = false
    }

    private fun deleteQuestion(position: Int) {
        if (position >= 0 && position < parsedQuestions.size) {
            parsedQuestions.removeAt(position)
            previewAdapter.updateData(parsedQuestions.toList())
            binding.tvPreviewCount.text = getString(R.string.format_ready_to_upload, parsedQuestions.size)

            if (parsedQuestions.isEmpty()) {
                hidePreview()
                binding.tvValidationResult.text = getString(R.string.msg_all_removed)
                binding.tvValidationResult.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
            } else {
                binding.tvValidationResult.text = getString(R.string.format_ready_to_upload, parsedQuestions.size)
                binding.tvValidationResult.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            }

            Toast.makeText(this, getString(R.string.msg_removed_q, parsedQuestions.size), Toast.LENGTH_SHORT).show()
        }
    }

    // ════════════════════════════════════════════════════
    //  UPLOAD TO FIREBASE (FIXED: Category Registration)
    // ════════════════════════════════════════════════════

    private fun uploadQuestions() {
        if (parsedQuestions.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_validate_first), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUpload.isEnabled = false
        binding.btnUpload.text = getString(R.string.status_uploading_dots)
        binding.layoutProgress.isVisible = true
        binding.layoutResult.isVisible = false
        binding.layoutPreview.isVisible = false
        binding.btnValidate.isEnabled = false

        val selectedType = when (binding.rgQuizType.checkedRadioButtonId) {
            R.id.rbImage -> "image"
            R.id.rbAudio -> "audio"
            R.id.rbVideo -> "video"
            else -> "text"
        }

        var uploaded = 0
        var failed = 0
        val total = parsedQuestions.size

        binding.tvProgress.text = getString(R.string.format_uploading_count, total)
        binding.tvProgressDetail.text = getString(R.string.format_progress_ratio, uploaded, total)

        // Collect all unique categories from the questions being uploaded
        val categoriesUsed = parsedQuestions
            .mapNotNull { it["category"] as? String }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        Log.d(TAG, "Uploading $total questions to categories: $categoriesUsed")

        for (q in parsedQuestions) {
            val category = (q["category"] as? String ?: selectedCategory).trim()

            Log.d(TAG, "Writing to path: questions/$category/items")

            db.collection("questions")
                .document(category)
                .collection("items")
                .add(q)
                .addOnSuccessListener { docRef ->
                    uploaded++
                    Log.d(TAG, "Uploaded ${docRef.id} to questions/$category/items")
                    binding.tvProgressDetail.text = getString(R.string.format_progress_ratio, uploaded, total)
                    if (uploaded + failed >= total) {
                        ensureCategoriesRegistered(categoriesUsed, uploaded, failed, selectedType)
                    }
                }
                .addOnFailureListener { e ->
                    failed++
                    Log.e(TAG, "Failed to upload to questions/$category/items: ${e.message}")
                    binding.tvProgressDetail.text = getString(R.string.format_progress_ratio, uploaded + failed, total)
                    if (uploaded + failed >= total) {
                        ensureCategoriesRegistered(categoriesUsed, uploaded, failed, selectedType)
                    }
                }
        }
    }

    /**
     * FIX: After upload, ensure every category used exists in the
     * "categories" collection so CategoryHelper can find them.
     * This uses set() with merge so it won't overwrite existing data.
     */
    private fun ensureCategoriesRegistered(
        categories: List<String>,
        uploaded: Int,
        failed: Int,
        type: String
    ) {
        if (categories.isEmpty()) {
            onUploadComplete(uploaded, failed)
            return
        }

        var catDone = 0
        val catTotal = categories.size

        for (catName in categories) {
            Log.d(TAG, "Registering category: $catName with type: $type")

            CategoryHelper.saveCategory(
                name = catName,
                imageUrl = "",
                description = "Uploaded via bulk upload",
                type = type
            ) { success ->
                catDone++
                if (catDone >= catTotal) {
                    onUploadComplete(uploaded, failed)
                }
            }
        }
    }

    private fun onUploadComplete(uploaded: Int, failed: Int) {
        binding.layoutProgress.isVisible = false
        binding.layoutResult.isVisible = true

        if (failed == 0) {
            binding.tvResultTitle.text = getString(R.string.title_complete)
            binding.tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            binding.tvResultDetail.text = getString(R.string.format_upload_success, uploaded)
            logAction("Bulk uploaded $uploaded questions via $currentTab")
        } else {
            binding.tvResultTitle.text = getString(R.string.title_partial_upload)
            binding.tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
            binding.tvResultDetail.text = getString(R.string.format_upload_partial, uploaded, failed, uploaded + failed)
        }

        binding.btnUpload.isEnabled = true
        binding.btnUpload.text = getString(R.string.btn_upload_to_firebase)
        binding.btnUpload.isVisible = false
        binding.btnValidate.isEnabled = true

        parsedQuestions.clear()
    }

    private fun logAction(details: String) {
        val log = hashMapOf(
            "action" to "bulk_upload",
            "details" to details,
            "adminEmail" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "timestamp" to Timestamp.now()
        )
        db.collection("admin").document("logs").collection("entries").add(log)
    }
}