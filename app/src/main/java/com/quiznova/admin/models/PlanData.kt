package com.quiznova.admin.models

data class PlanData(
    val id: String = "",
    val type: String = "",
    val label: String = "",
    val days: Int = 0,
    val coinPrice: Int = 0,
    val moneyPrice: String = "",
    val moneyPriceRaw: Long = 0L
)
