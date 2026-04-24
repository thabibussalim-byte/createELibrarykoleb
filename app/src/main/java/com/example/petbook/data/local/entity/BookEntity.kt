package com.example.petbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books") data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val judulBuku: String,
    val deskripsi: String?,
    val stok: Int,
    val tanggalTerbit: String?,
    val foto: String?,
    val genreId: String?,
    val penulisId: String?,
    val penerbitId: String?,
    val createdAt: String?,
    val updatedAt: String?
)