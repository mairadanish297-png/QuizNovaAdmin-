package com.quiznova.admin.models

data class ChapterData(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val content_en: String = "",
    val content_ur: String = "",
    val order: Int = 0
)