package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class UpdateMahasantriRequest(
    @SerializedName("nama_mahasantri") val namaMahasantri: String,
    @SerializedName("jurusan") val jurusan: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("user_id") val userId: Int // Tambahkan userId untuk memastikan server menerima data yang lengkap
)