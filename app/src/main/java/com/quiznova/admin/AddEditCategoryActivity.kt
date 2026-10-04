package com.quiznova.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.quiznova.admin.databinding.ActivityAddEditCategoryBinding
import com.quiznova.admin.firebase.CategoryHelper

class AddEditCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCategoryBinding

    private var isEditMode = false
    private var existingName = ""
    private var existingImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if edit mode
        existingName = intent.getStringExtra("categoryName") ?: ""
        val existingImage = intent.getStringExtra("categoryImage") ?: ""
        val existingIcon = intent.getStringExtra("categoryIcon") ?: ""
        val existingDesc = intent.getStringExtra("categoryDesc") ?: ""
        val existingColor = intent.getStringExtra("categoryColor") ?: "#6200EE"
        val existingEmoji = intent.getStringExtra("categoryEmoji") ?: "📝"
        val existingType = intent.getStringExtra("categoryType") ?: "text"
        isEditMode = existingName.isNotEmpty()

        binding.includeHeader.tvTitle.text = if (isEditMode) getString(R.string.title_edit_category) else getString(R.string.title_add_category)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        // Pre-fill if editing
        if (isEditMode) {
            binding.etCategoryName.setText(existingName)
            binding.etCategoryName.isEnabled = false // Don't allow renaming (it's the doc ID)
            binding.etDescription.setText(existingDesc)
            binding.etColor.setText(existingColor)
            binding.etEmoji.setText(existingEmoji)
            
            when (existingType.replace(" quiz", "")) {
                "image" -> binding.rbImage.isChecked = true
                "audio" -> binding.rbAudio.isChecked = true
                "video" -> binding.rbVideo.isChecked = true
                else -> binding.rbText.isChecked = true
            }
            
            existingImageUrl = existingIcon.ifEmpty { existingImage }

            if (existingImageUrl.isNotEmpty()) {
                binding.etImageUrl.setText(existingImageUrl)
                updatePreview(existingImageUrl)
            }
        }

        setupPreviewLogic()

        binding.btnSave.setOnClickListener {
            saveCategory()
        }
    }

    private fun setupPreviewLogic() {
        binding.etImageUrl.addTextChangedListener(object : TextWatcher {
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
                .placeholder(R.drawable.ic_category_placeholder)
                .error(R.drawable.ic_category_placeholder)
                .into(binding.ivLogoPreview)
        } else {
            binding.ivLogoPreview.setImageResource(R.drawable.ic_category_placeholder)
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val color = binding.etColor.text.toString().trim().ifEmpty { "#6200EE" }
        val emoji = binding.etEmoji.text.toString().trim().ifEmpty { "📝" }
        val imageUrl = binding.etImageUrl.text.toString().trim()
        
        val type = when (binding.rgQuizType.checkedRadioButtonId) {
            R.id.rbImage -> "image"
            R.id.rbAudio -> "audio"
            R.id.rbVideo -> "video"
            else -> "text"
        }

        if (name.isEmpty()) {
            binding.etCategoryName.error = getString(R.string.msg_name_required)
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressSave.visibility = View.VISIBLE
        binding.tvSaveStatus.visibility = View.GONE

        saveToFirestore(name, imageUrl, description, color, emoji, type)
    }



    private fun saveToFirestore(name: String, imageUrl: String, description: String, color: String, emoji: String, type: String) {
        binding.tvSaveStatus.visibility = View.VISIBLE
        binding.tvSaveStatus.text = getString(R.string.status_saving_dot)

        CategoryHelper.saveCategory(name, imageUrl, description, color, emoji, type) { success ->
            runOnUiThread {
                binding.progressSave.visibility = View.GONE
                binding.btnSave.isEnabled = true

                if (success) {
                    binding.tvSaveStatus.text = getString(R.string.msg_save_success)
                    binding.tvSaveStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_success)
                    )
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

                    // Close after short delay
                    binding.tvSaveStatus.postDelayed({ finish() }, 800)
                } else {
                    binding.tvSaveStatus.text = getString(R.string.msg_save_failed)
                    binding.tvSaveStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_error)
                    )
                }
            }
        }
    }
}