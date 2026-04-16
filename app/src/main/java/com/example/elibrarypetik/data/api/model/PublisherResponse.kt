package com.example.elibrarypetik.data.api.model

import com.google.gson.annotations.SerializedName

data class PublisherResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<PublisherItem>
)

data class PublisherItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_penerbit") val publisherName: String,
    @SerializedName("email") val email: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("profil") val profil: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)