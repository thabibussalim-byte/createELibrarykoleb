package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class UpdateUserRequest(
    @SerializedName("profil") val profil: String? = null,
    @SerializedName("password") val password: String? = null

)
