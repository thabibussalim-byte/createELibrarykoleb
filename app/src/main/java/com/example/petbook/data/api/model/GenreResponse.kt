package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class GenreResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<GenreItem>
)

data class GenreItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_genre") val namaGenre: String,
    @SerializedName("deskripsi") val deskripsi: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)