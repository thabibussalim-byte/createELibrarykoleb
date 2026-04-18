package com.example.petbook.data.api.model

import com.google.gson.annotations.SerializedName

data class HistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<HistoryDataItem>
)

data class HistoryDataItem(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("tgl_pinjam") val tglPinjam: String,
    @SerializedName("tgl_kembali") val tglKembali: String,
    @SerializedName("keterangan") val keterangan: String?,
    @SerializedName("buku_id") val bukuId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("denda") val denda: Int? = 0
)