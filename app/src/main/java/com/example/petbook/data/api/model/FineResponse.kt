package com.example.petbook.data.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class FineResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<FineDataItem>
)

@Parcelize
data class FineDataItem(
    @SerializedName("id") val id: Int,
    @SerializedName("total_denda") val totalDenda: String,
    @SerializedName("status") val status: String,
    @SerializedName("transaksi_id") val transaksiId: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
) : Parcelable