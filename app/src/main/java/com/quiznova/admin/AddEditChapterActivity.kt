package com.quiznova.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.quiznova.admin.databinding.ActivityAddEditChapterBinding
import com.quiznova.admin.firebase.BookHelper

class AddEditChapterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditChapterBinding

    private var isEditMode = false
    private var categoryName = ""
    private var bookId = ""
    private var bookTitle = ""
    private var chapterId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        categoryName = intent.getStringExtra("categoryName") ?: ""
        bookId = intent.getStringExtra("bookId") ?: ""
        bookTitle = intent.getStringExtra("bookTitle") ?: ""
        chapterId = intent.getStringExtra("chapterId") ?: ""
        isEditMode = chapterId.isNotEmpty()

        binding.includeHeader.tvTitle.text = if (isEditMode) "Edit Chapter" else "Add Chapter"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        if (isEditMode) {
            binding.etChapterTitle.setText(intent.getStringExtra("chapterTitle") ?: "")
            binding.etChapterContentEN.setText(intent.getStringExtra("chapterContentEN") ?: intent.getStringExtra("chapterContent") ?: "")
            binding.etChapterContentUR.setText(intent.getStringExtra("chapterContentUR") ?: "")
            binding.etChapterOrder.setText((intent.getIntExtra("chapterOrder", 0)).toString())
        }

        setupButtons()
        setupTextWatchers()
    }

    private fun setupButtons() {
        binding.btnInsertAdEN.setOnClickListener { insertAdTag(binding.etChapterContentEN) }
        binding.btnInsertAdUR.setOnClickListener { insertAdTag(binding.etChapterContentUR) }

        binding.btnSave.setOnClickListener { saveChapter() }
    }

    private fun insertAdTag(editText: android.widget.EditText) {
        val adTag = "{native_ad}"
        var start = editText.selectionStart
        var end = editText.selectionEnd

        // If no selection/focus, insert at the end
        if (start < 0) {
            start = editText.text.length
            end = editText.text.length
        }

        val originalText = editText.text.toString()
        val newText = originalText.substring(0, start) + adTag + originalText.substring(end)

        editText.setText(newText)
        editText.setSelection(start + adTag.length)
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val lenEN = binding.etChapterContentEN.text?.length ?: 0
                val lenUR = binding.etChapterContentUR.text?.length ?: 0
                binding.tvCharCount.text = "EN: $lenEN | UR: $lenUR characters"
            }
        }
        binding.etChapterContentEN.addTextChangedListener(watcher)
        binding.etChapterContentUR.addTextChangedListener(watcher)
    }

    private fun saveChapter() {
        val title = binding.etChapterTitle.text.toString().trim()
        val contentEN = binding.etChapterContentEN.text.toString().trim()
        val contentUR = binding.etChapterContentUR.text.toString().trim()
        val order = binding.etChapterOrder.text.toString().trim().toIntOrNull() ?: 0

        if (title.isEmpty()) {
            binding.etChapterTitle.error = "Title required"
            return
        }

        if (contentEN.isEmpty() && contentUR.isEmpty()) {
            Toast.makeText(this, "Please enter content in at least one language", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressSave.visibility = View.VISIBLE
        binding.tvSaveStatus.visibility = View.VISIBLE
        binding.tvSaveStatus.text = "Saving..."

        val id = if (isEditMode) chapterId else null

        BookHelper.saveChapter(categoryName, bookId, id, title, contentEN, contentUR, order) { success ->
            runOnUiThread {
                binding.progressSave.visibility = View.GONE
                binding.btnSave.isEnabled = true

                if (success) {
                    binding.tvSaveStatus.text = "Chapter saved!"
                    binding.tvSaveStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_success)
                    )
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    binding.tvSaveStatus.postDelayed({ finish() }, 800)
                } else {
                    binding.tvSaveStatus.text = "Save failed. Try again."
                    binding.tvSaveStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_error)
                    )
                }
            }
        }
    }
}