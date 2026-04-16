package com.example.elibrarypetik.data.api.model

import com.google.gson.annotations.SerializedName

data class AuthorResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<AuthorItem>
)

data class AuthorItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_penulis") val namaPenulis: String
)