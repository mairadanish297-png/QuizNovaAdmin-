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
import com.quiznova.admin.databinding.ActivityManageCategoriesBinding
import com.quiznova.admin.firebase.CategoryHelper
import com.quiznova.admin.models.CategoryData

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageCategoriesBinding
    private val categories = mutableListOf<CategoryData>()
    private lateinit var adapter: CategoryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.tvTitle.text = getString(R.string.title_manage_categories)
        binding.includeHeader.btnBack.setOnClickListener { finish() }

        adapter = CategoryListAdapter(categories,
            onEdit = { cat ->
                val intent = Intent(this, AddEditCategoryActivity::class.java)
                intent.putExtra("categoryName", cat.name)
                intent.putExtra("categoryImage", cat.imageUrl)
                intent.putExtra("categoryIcon", cat.icon)
                intent.putExtra("categoryDesc", cat.description)
                intent.putExtra("categoryColor", cat.color)
                intent.putExtra("categoryEmoji", cat.emoji)
                intent.putExtra("categoryType", cat.type)
                startActivity(intent)
            },
            onDelete = { cat -> confirmDelete(cat) },
            onBooks = { cat ->
                if (cat.name.isEmpty()) {
                    Toast.makeText(this, "Category name is missing", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, ManageBooksActivity::class.java)
                    intent.putExtra("categoryName", cat.name)
                    startActivity(intent)
                }
            }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        binding.btnAddCategory.setOnClickListener {
            startActivity(Intent(this, AddEditCategoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        // First fix existing categories schema in Firestore if needed, then load
        CategoryHelper.fixExistingCategories {
            CategoryHelper.clearCache()
            CategoryHelper.getCategoriesWithDetails { cats ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    categories.clear()
                    categories.addAll(cats)
                    adapter.notifyDataSetChanged()
                    binding.tvCatCount.text = getString(R.string.format_categories_count, cats.size)
                    binding.tvEmpty.visibility = if (cats.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun confirmDelete(cat: CategoryData) {
        AlertDialog.Builder(this, R.style.Theme_QuizNovaAdmin_Dialog)
            .setTitle(getString(R.string.title_delete_category))
            .setMessage(getString(R.string.msg_delete_category_confirm, cat.name))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                CategoryHelper.deleteCategory(cat.id) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, getString(R.string.msg_deleted_cat, cat.name), Toast.LENGTH_SHORT).show()
                            loadCategories()
                        } else {
                            Toast.makeText(this, getString(R.string.msg_delete_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}

// ═══════════════════════════════════════
//  CATEGORY LIST ADAPTER
// ═══════════════════════════════════════

class CategoryListAdapter(
    private val categories: List<CategoryData>,
    private val onEdit: (CategoryData) -> Unit,
    private val onDelete: (CategoryData) -> Unit,
    private val onBooks: (CategoryData) -> Unit
) : RecyclerView.Adapter<CategoryListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivLogo: ImageView = view.findViewById(R.id.ivCategoryLogo)
        val tvName: TextView = view.findViewById(R.id.tvCatName)
        val tvDesc: TextView = view.findViewById(R.id.tvCatDesc)
        val tvQuestions: TextView = view.findViewById(R.id.tvCatQuestions)
        val btnEdit: TextView = view.findViewById(R.id.btnEditCat)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteCat)
        val btnBooks: TextView = view.findViewById(R.id.btnManageBooks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = categories[position]
        holder.tvName.text = cat.name
        holder.tvDesc.text = cat.description.ifEmpty { holder.itemView.context.getString(R.string.label_no_description) }
        holder.tvQuestions.text = holder.itemView.context.getString(R.string.format_questions_count, cat.questionCount)

        if (cat.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(cat.imageUrl)
                .placeholder(R.drawable.ic_category_placeholder)
                .error(R.drawable.ic_category_placeholder)
                .centerCrop()
                .into(holder.ivLogo)
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_category_placeholder)
        }

        holder.btnEdit.setOnClickListener { onEdit(cat) }
        holder.btnDelete.setOnClickListener { onDelete(cat) }
        holder.btnBooks.setOnClickListener { onBooks(cat) }
    }

    override fun getItemCount() = categories.size
}