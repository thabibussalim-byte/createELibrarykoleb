package com.example.petbook.data.api.model


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize



data class BookResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<BookItem>
)

@Parcelize
data class BookItem(
    @SerializedName("id") val id: Int,
    @SerializedName("judul_buku") val judulBuku: String,
    @SerializedName("deskripsi") val deskripsi: String,
    @SerializedName("stok") val stok: Int,
    @SerializedName("tgl_terbit") val tglTerbit: String,
    @SerializedName("foto") val foto: String,
    @SerializedName("genre_id") val genreId: Int,
    @SerializedName("penulis_id") val penulisId: Int,
    @SerializedName("penerbit_id") val penerbitId: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
) : Parcelable