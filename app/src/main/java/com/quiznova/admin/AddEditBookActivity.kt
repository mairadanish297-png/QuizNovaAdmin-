package com.quiznova.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.quiznova.admin.databinding.ActivityAddEditBookBinding
import com.quiznova.admin.firebase.BookHelper

class AddEditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBookBinding

    private var isEditMode = false
    private var categoryName = ""
    private var bookId = ""
    private var existingCoverUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        categoryName = intent.getStringExtra("categoryName") ?: ""
        bookId = intent.getStringExtra("bookId") ?: ""
        isEditMode = bookId.isNotEmpty()

        binding.includeHeader.tvTitle.text = if (isEditMode) "Edit Book" else "Add Book"
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        if (isEditMode) {
            binding.etBookTitle.setText(intent.getStringExtra("bookTitle") ?: "")
            binding.etBookDesc.setText(intent.getStringExtra("bookDesc") ?: "")
            binding.etBookOrder.setText((intent.getIntExtra("bookOrder", 0)).toString())
            existingCoverUrl = intent.getStringExtra("bookCover") ?: ""

            if (existingCoverUrl.isNotEmpty()) {
                binding.etCoverUrl.setText(existingCoverUrl)
                updatePreview(existingCoverUrl)
            }
        }

        setupPreviewLogic()
        binding.btnSave.setOnClickListener { saveBook() }
    }

    private fun setupPreviewLogic() {
        binding.etCoverUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview(s.toString().trim())
            }
        })
    }

    private fun updatePreview(url: String) {
        if (url.isNotEmpty()) {
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .into(binding.ivCoverPreview)
        } else {
            binding.ivCoverPreview.setImageResource(R.drawable.ic_book_placeholder)
        }
    }

    private fun saveBook() {
        val title = binding.etBookTitle.text.toString().trim()
        val desc = binding.etBookDesc.text.toString().trim()
        val order = binding.etBookOrder.text.toString().trim().toIntOrNull() ?: 0

        if (title.isEmpty()) {
            binding.etBookTitle.error = "Title required"
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressSave.visibility = View.VISIBLE
        binding.tvSaveStatus.visibility = View.GONE

        val coverUrl = binding.etCoverUrl.text.toString().trim()
        doSaveFirestore(title, desc, order, coverUrl)
    }



    private fun doSaveFirestore(title: String, desc: String, order: Int, coverUrl: String) {
        binding.tvSaveStatus.visibility = View.VISIBLE
        binding.tvSaveStatus.text = "Saving..."

        val id = if (isEditMode) bookId else null

        BookHelper.saveBook(categoryName, id, title, coverUrl, desc, order) { success, _ ->
            runOnUiThread {
                binding.progressSave.visibility = View.GONE
                binding.btnSave.isEnabled = true

                if (success) {
                    binding.tvSaveStatus.text = "Book saved!"
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