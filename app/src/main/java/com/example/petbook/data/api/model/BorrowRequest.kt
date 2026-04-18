package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class BorrowRequest(
    @SerializedName("tgl_pinjam") val tglPinjam: String,
    @SerializedName("tgl_kembali") val tglKembali: String,
    @SerializedName("buku_id") val bukuId: Int
)

data class BorrowResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: BorrowData? = null,
    @SerializedName("errors") val errors: List<BorrowError>? = null
)

data class BorrowError(
    @SerializedName("field") val field: String,
    @SerializedName("message") val message: String
)

data class BorrowData(
    @SerializedName("status") val status: String,
    @SerializedName("id") val id: Int,
    @SerializedName("tgl_pinjam") val tglPinjam: String,
    @SerializedName("tgl_kembali") val tglKembali: String,
    @SerializedName("buku_id") val bukuId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("createdAt") val createdAt: String
)