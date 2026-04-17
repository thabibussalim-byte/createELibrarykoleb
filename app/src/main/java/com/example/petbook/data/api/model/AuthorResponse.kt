package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class AuthorResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<AuthorItem>
)

data class AuthorItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_penulis") val namaPenulis: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("email") val email: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("profil") val profil: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)