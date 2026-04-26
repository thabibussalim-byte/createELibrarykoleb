package com.example.petbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: Int,
    val judulBuku: String,
    val deskripsi: String?,
    val stok: Int,
    val tanggalTerbit: String?,
    val foto: String?,
    val genreId: Int?,
    val penulisId: Int?,
    val penerbitId: Int?,
    val createdAt: String?,
    val updatedAt: String?,

)
