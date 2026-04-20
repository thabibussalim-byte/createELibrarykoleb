package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
	@SerializedName("status") val status: String,
	@SerializedName("message") val message: String,
	@SerializedName("data") val data: List<UserItem>
)

data class UserItem(
	@SerializedName("id") val id: Int,
	@SerializedName("username") val username: String,
	@SerializedName("profil") val profil: String,
	@SerializedName("role") val role: String,
	@SerializedName("createdAt") val createdAt: String,
	@SerializedName("updatedAt") val updatedAt: String
)

