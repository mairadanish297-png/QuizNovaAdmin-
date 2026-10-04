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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quiznova.admin.databinding.ActivityManageChaptersBinding
import com.quiznova.admin.firebase.BookHelper
import com.quiznova.admin.models.ChapterData

class ManageChaptersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageChaptersBinding
    private val chapters = mutableListOf<ChapterData>()
    private lateinit var adapter: ChapterListAdapter
    private var categoryName = ""
    private var bookId = ""
    private var bookTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageChaptersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        categoryName = intent.getStringExtra("categoryName") ?: ""
        bookId = intent.getStringExtra("bookId") ?: ""
        bookTitle = intent.getStringExtra("bookTitle") ?: ""

        if (categoryName.isEmpty() || bookId.isEmpty()) {
            Toast.makeText(this, "Error: Missing book or category data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeHeader.tvTitle.text = "Chapters"
        binding.includeHeader.btnBack.setOnClickListener { finish() }
        binding.tvBookNameBanner.text = bookTitle
        binding.tvCategoryNameBanner.text = "Category: $categoryName"

        adapter = ChapterListAdapter(chapters,
            onEdit = { ch -> editChapter(ch) },
            onDelete = { ch -> confirmDelete(ch) }
        )

        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        binding.rvChapters.adapter = adapter

        binding.btnAddChapter.setOnClickListener {
            val intent = Intent(this, AddEditChapterActivity::class.java)
            intent.putExtra("categoryName", categoryName)
            intent.putExtra("bookId", bookId)
            intent.putExtra("bookTitle", bookTitle)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadChapters()
    }

    private fun loadChapters() {
        if (categoryName.isEmpty() || bookId.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        BookHelper.getChapters(categoryName, bookId) { list ->
            if (!isDestroyed && !isFinishing) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    chapters.clear()
                    chapters.addAll(list)
                    adapter.notifyDataSetChanged()
                    binding.tvChapterCount.text = "${list.size} chapters"
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun editChapter(ch: ChapterData) {
        val intent = Intent(this, AddEditChapterActivity::class.java)
        intent.putExtra("categoryName", categoryName)
        intent.putExtra("bookId", bookId)
        intent.putExtra("bookTitle", bookTitle)
        intent.putExtra("chapterId", ch.id)
        intent.putExtra("chapterTitle", ch.title)
        intent.putExtra("chapterContentEN", ch.content_en)
        intent.putExtra("chapterContentUR", ch.content_ur)
        intent.putExtra("chapterOrder", ch.order)
        startActivity(intent)
    }

    private fun confirmDelete(ch: ChapterData) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Delete Chapter?")
            .setMessage("Delete \"${ch.title}\"?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                BookHelper.deleteChapter(categoryName, bookId, ch.id) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            loadChapters()
                        } else {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ═══════════════════════════════════════
//  CHAPTER LIST ADAPTER
// ═══════════════════════════════════════

class ChapterListAdapter(
    private val chapters: List<ChapterData>,
    private val onEdit: (ChapterData) -> Unit,
    private val onDelete: (ChapterData) -> Unit
) : RecyclerView.Adapter<ChapterListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrder: TextView = view.findViewById(R.id.tvChapterOrder)
        val tvTitle: TextView = view.findViewById(R.id.tvChapterTitle)
        val tvPreview: TextView = view.findViewById(R.id.tvChapterPreview)
        val btnEdit: TextView = view.findViewById(R.id.btnEditChapter)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteChapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = chapters[position]
        holder.tvOrder.text = "${ch.order}"
        holder.tvTitle.text = ch.title
        holder.tvPreview.text = ch.content.take(80).ifEmpty { "No content yet" }

        holder.btnEdit.setOnClickListener { onEdit(ch) }
        holder.btnDelete.setOnClickListener { onDelete(ch) }
    }

    override fun getItemCount() = chapters.size
}