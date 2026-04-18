package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class FineResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<FineDataItem>
)

data class FineDataItem(
    @SerializedName("id") val id: Int,
    @SerializedName("total_denda") val totalDenda: String,
    @SerializedName("status") val status: String,
    @SerializedName("transaksi_id") val transaksiId: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)