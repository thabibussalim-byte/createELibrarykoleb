package com.example.elibrarypetik.data.model

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val imageUrl: String, // Menggunakan String untuk menampung link URL
    val rating: Float
)