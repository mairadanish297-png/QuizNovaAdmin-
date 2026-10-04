package com.quiznova.admin.models

data class QuestionData(
    var id: String = "",
    val en: QuestionLang = QuestionLang(),
    val ur: QuestionLang = QuestionLang(),
    val roman: QuestionLang = QuestionLang(),
    val correct: Int = 0,
    val difficulty: String = "easy",
    val category: String = "Science",
    val type: String = "mcq",
    val tags: List<String> = listOf(),
    val hasImage: Boolean = false,
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

data class QuestionLang(
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val explanation: String = ""
)