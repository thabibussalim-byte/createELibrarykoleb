package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class MahasantriUpdateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: MahasantriDataItem // Berupa Objek Tunggal, bukan List
)