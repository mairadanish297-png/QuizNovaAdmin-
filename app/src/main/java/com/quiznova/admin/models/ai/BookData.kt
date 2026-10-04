package com.quiznova.admin.models

data class BookData(
    val id: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val description: String = "",
    val order: Int = 0,
    val chapterCount: Int = 0
)