package com.example.elibrarypetik.data.model

data class HistoryItem(
    val id: Int,
    val title: String,
    val author: String,
    val imageUrl: String,
    val statusDate: String,
    val fine: String? = null,
    val isLate: Boolean = false
)