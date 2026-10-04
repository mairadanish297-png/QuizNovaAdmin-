package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.quiznova.admin.databinding.ActivityManageBooksBinding
import com.quiznova.admin.firebase.BookHelper
import com.quiznova.admin.models.BookData

class ManageBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBooksBinding
    private val books = mutableListOf<BookData>()
    private lateinit var adapter: BookListAdapter
    private var categoryName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        categoryName = intent.getStringExtra("categoryName") ?: ""
        
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Error: Category name is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeHeader.tvTitle.text = "Books"
        binding.includeHeader.btnBack.setOnClickListener { finish() }
        binding.tvCategoryBanner.text = "Category: $categoryName"

        adapter = BookListAdapter(books,
            onChapters = { book -> openChapters(book) },
            onEdit = { book -> editBook(book) },
            onDelete = { book -> confirmDelete(book) }
        )

        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = adapter

        binding.btnAddBook.setOnClickListener {
            val intent = Intent(this, AddEditBookActivity::class.java)
            intent.putExtra("categoryName", categoryName)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun loadBooks() {
        if (categoryName.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        BookHelper.getBooks(categoryName) { bookList ->
            if (!isDestroyed && !isFinishing) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    books.clear()
                    books.addAll(bookList)
                    adapter.notifyDataSetChanged()
                    binding.tvBookCount.text = "${bookList.size} books"
                    binding.tvEmpty.visibility = if (bookList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun openChapters(book: BookData) {
        val intent = Intent(this, ManageChaptersActivity::class.java)
        intent.putExtra("categoryName", categoryName)
        intent.putExtra("bookId", book.id)
        intent.putExtra("bookTitle", book.title)
        startActivity(intent)
    }

    private fun editBook(book: BookData) {
        val intent = Intent(this, AddEditBookActivity::class.java)
        intent.putExtra("categoryName", categoryName)
        intent.putExtra("bookId", book.id)
        intent.putExtra("bookTitle", book.title)
        intent.putExtra("bookCover", book.coverUrl)
        intent.putExtra("bookDesc", book.description)
        intent.putExtra("bookOrder", book.order)
        startActivity(intent)
    }

    private fun confirmDelete(book: BookData) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle("Delete Book?")
            .setMessage("Delete \"${book.title}\" and ALL its chapters?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                BookHelper.deleteBook(categoryName, book.id) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            loadBooks()
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
//  BOOK LIST ADAPTER
// ═══════════════════════════════════════

class BookListAdapter(
    private val books: List<BookData>,
    private val onChapters: (BookData) -> Unit,
    private val onEdit: (BookData) -> Unit,
    private val onDelete: (BookData) -> Unit
) : RecyclerView.Adapter<BookListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivBookCover)
        val tvTitle: TextView = view.findViewById(R.id.tvBookTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvBookDesc)
        val tvChapters: TextView = view.findViewById(R.id.tvBookChapters)
        val btnChapters: TextView = view.findViewById(R.id.btnChapters)
        val btnEdit: TextView = view.findViewById(R.id.btnEditBook)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val book = books[position]
        holder.tvTitle.text = book.title
        holder.tvDesc.text = book.description.ifEmpty { "No description" }
        holder.tvChapters.text = "Order: ${book.order}"

        if (book.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .centerCrop()
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_book_placeholder)
        }

        holder.btnChapters.setOnClickListener { onChapters(book) }
        holder.btnEdit.setOnClickListener { onEdit(book) }
        holder.btnDelete.setOnClickListener { onDelete(book) }
    }

    override fun getItemCount() = books.size
}