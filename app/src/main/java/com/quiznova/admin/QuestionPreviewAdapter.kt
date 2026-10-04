package com.quiznova.admin

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class QuestionPreviewAdapter(
    private var questions: MutableList<Map<String, Any>>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<QuestionPreviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestionNumber: TextView = view.findViewById(R.id.tvQuestionNumber)
        val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
        val tvCategoryTag: TextView = view.findViewById(R.id.tvCategoryTag)
        val btnDeleteQuestion: TextView = view.findViewById(R.id.btnDeleteQuestion)
        val tvQuestionText: TextView = view.findViewById(R.id.tvQuestionText)
        val tvOptionALetter: TextView = view.findViewById(R.id.tvOptionALetter)
        val tvOptionA: TextView = view.findViewById(R.id.tvOptionA)
        val tvOptionBLetter: TextView = view.findViewById(R.id.tvOptionBLetter)
        val tvOptionB: TextView = view.findViewById(R.id.tvOptionB)
        val tvOptionCLetter: TextView = view.findViewById(R.id.tvOptionCLetter)
        val tvOptionC: TextView = view.findViewById(R.id.tvOptionC)
        val tvOptionDLetter: TextView = view.findViewById(R.id.tvOptionDLetter)
        val tvOptionD: TextView = view.findViewById(R.id.tvOptionD)
        val tvExplanation: TextView = view.findViewById(R.id.tvExplanation)
        val btnToggleExplanation: TextView = view.findViewById(R.id.btnToggleExplanation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val q = questions[position]
        val context = holder.itemView.context

        // Question number
        holder.tvQuestionNumber.text = context.getString(R.string.format_q_number, position + 1)

        // Difficulty
        val difficulty = q["difficulty"] as? String ?: "easy"
        holder.tvDifficulty.text = difficulty
        val diffColor = when (difficulty) {
            "easy" -> "#2E7D32".toColorInt()
            "medium" -> "#F57F17".toColorInt()
            "hard" -> "#C62828".toColorInt()
            else -> "#455A64".toColorInt()
        }
        holder.tvDifficulty.setBackgroundColor(diffColor)

        // Category
        holder.tvCategoryTag.text = q["category"] as? String ?: context.getString(R.string.filter_all)

        // English question + options
        @Suppress("UNCHECKED_CAST")
        val en = q["en"] as? Map<String, Any> ?: emptyMap()
        val questionText = en["question"] as? String ?: ""
        val options = en["options"] as? List<String> ?: listOf()
        val explanation = en["explanation"] as? String ?: ""
        val correct = (q["correct"] as? Number)?.toInt() ?: (q["correct"] as? String)?.toIntOrNull() ?: 0

        holder.tvQuestionText.text = questionText

        // Options with correct highlight
        val optionLetters = listOf(holder.tvOptionALetter, holder.tvOptionBLetter, holder.tvOptionCLetter, holder.tvOptionDLetter)
        val optionTexts = listOf(holder.tvOptionA, holder.tvOptionB, holder.tvOptionC, holder.tvOptionD)

        val successColor = "#4CAF50".toColorInt()

        for (i in 0..3) {
            if (i < options.size) {
                optionLetters[i].isVisible = true
                optionTexts[i].isVisible = true
                optionTexts[i].text = options[i]

                if (i == correct) {
                    optionTexts[i].setTextColor(successColor)
                    optionTexts[i].setTypeface(null, Typeface.BOLD)
                    optionLetters[i].setBackgroundColor(successColor)
                    optionLetters[i].setTextColor(ContextCompat.getColor(context, android.R.color.white))
                } else {
                    optionTexts[i].setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    optionTexts[i].setTypeface(null, Typeface.NORMAL)
                    optionLetters[i].setBackgroundColor(ContextCompat.getColor(context, R.color.bg_surface))
                    optionLetters[i].setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
            } else {
                optionLetters[i].isGone = true
                optionTexts[i].isGone = true
            }
        }

        // Explanation toggle
        if (explanation.isNotEmpty()) {
            holder.btnToggleExplanation.isVisible = true
            holder.tvExplanation.text = explanation

            holder.btnToggleExplanation.setOnClickListener {
                if (holder.tvExplanation.isGone) {
                    holder.tvExplanation.isVisible = true
                    holder.btnToggleExplanation.text = context.getString(R.string.btn_hide_explanation)
                } else {
                    holder.tvExplanation.isGone = true
                    holder.btnToggleExplanation.text = context.getString(R.string.btn_show_explanation)
                }
            }
        } else {
            holder.btnToggleExplanation.isGone = true
            holder.tvExplanation.isGone = true
        }

        // Delete button
        holder.btnDeleteQuestion.setOnClickListener {
            onDelete(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = questions.size

    fun updateData(newQuestions: List<Map<String, Any>>) {
        questions.clear()
        questions.addAll(newQuestions)
        notifyDataSetChanged()
    }

    fun getQuestions(): MutableList<Map<String, Any>> = questions
}