package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class UpdateBookResponse(

	@SerializedName("data") val data: Data? = null,

	@SerializedName("message") val message: String? = null,

	@SerializedName("status") val status: String? = null
)

data class DataBook(
	@SerializedName("createdAt") val createdAt: String? = null,
	@SerializedName("foto") val foto: String? = null,
	@SerializedName("penulis_id") val penulisId: String? = null,
	@SerializedName("penerbit_id") val penerbitId: String? = null,
	@SerializedName("tgl_terbit") val tglTerbit: String? = null,
	@SerializedName("id") val id: Int? = null,
	@SerializedName("deskripsi") val deskripsi: String? = null,
	@SerializedName("stok") val stok: String? = null,
	@SerializedName("judul_buku") val judulBuku: String? = null,
	@SerializedName("genre_id") val genreId: String? = null,
	@SerializedName("updatedAt") val updatedAt: String? = null
)
