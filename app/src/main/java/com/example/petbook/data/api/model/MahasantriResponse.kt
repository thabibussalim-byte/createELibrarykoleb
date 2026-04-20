package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class MahasantriResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<MahasantriDataItem>
)

data class MahasantriDataItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_mahasantri") val namaMahasantri: String,
    @SerializedName("jurusan") val jurusan: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("user_id") val userId: Int
)