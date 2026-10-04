package com.quiznova.admin.models

data class CategoryData(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val icon: String = "",
    val description: String = "",
    val questionCount: Int = 0,
    val color: String = "#6200EE",
    val emoji: String = "📝",
    val type: String = "text",
    val quizType: String = "text quiz",
    val categoryEmoji: String = "📝",
    val categoryColor: String = "#6200EE"
)
