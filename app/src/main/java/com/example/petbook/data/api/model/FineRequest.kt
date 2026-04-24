package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class FineRequest(
    @SerializedName("total_denda") val totalDenda: String,
    @SerializedName("status") val status: String = "belumdibayar",
    @SerializedName("transaksi_id") val transaksiId: Int
)
