package com.example.petbook.data.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class HistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<HistoryDataItem>
)

@Parcelize
data class HistoryDataItem(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("tgl_pinjam") val tglPinjam: String,
    @SerializedName("tgl_kembali") val tglKembali: String,
    @SerializedName("keterangan") val keterangan: String?,
    @SerializedName("buku_id") val bukuId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("denda") val denda: Int? = 0,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
) : Parcelable