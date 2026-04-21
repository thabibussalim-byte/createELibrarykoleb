package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class UserRequest(
	@SerializedName("data") val data: Data,
	@SerializedName("status") val status: String,
	@SerializedName("message") val message: String
)

data class Data(
	@SerializedName("id") val id: Int,
	@SerializedName("username") val username: String,
	@SerializedName("password") val password: String,
	@SerializedName("jurusan") val jurusan: String,
	@SerializedName("alamat") val alamat: String,
	@SerializedName("no_hp") val noHp: String,
	@SerializedName("role") val role: String,
	@SerializedName("createdAt") val createdAt: String,
	@SerializedName("updatedAt") val updatedAt: String
)

