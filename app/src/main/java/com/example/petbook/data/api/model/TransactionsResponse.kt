package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class TransactionsResponse(
	@SerializedName("status") val status: String,
	@SerializedName("message") val message: String,
	@SerializedName("data") val data: List<TransactionsItem>
)

data class TransactionsItem(
	@SerializedName("id") val id: Int,
	@SerializedName("buku_id") val bukuId: Int,
	@SerializedName("keterangan") val keterangan: String,
	@SerializedName("tgl_pinjam") val tglPinjam: String,
	@SerializedName("tgl_kembali") val tglKembali: String,
	@SerializedName("status") val status: String,
	@SerializedName("createdAt") val createdAt: String,
	@SerializedName("updatedAt") val updatedAt: String,
	@SerializedName("user_id") val userId: Int
)

