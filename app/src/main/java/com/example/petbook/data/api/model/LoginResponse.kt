package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LoginData?
)

data class LoginData(
    @SerializedName("id") val id: Int? = null, // Diubah jadi nullable
    @SerializedName("token") val token: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("profil") val profil: String? = null
)