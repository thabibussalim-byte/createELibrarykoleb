package com.example.petbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "histories")
data class HistoryEntity(
    @PrimaryKey
    val id: Int,
    val status: String,
    val tanggalPinjam: String,
    val tanggalPengembalian: String?,
    val keterangan: String?,
    val bukuId: Int,
    val userId: Int,
    val denda: Int?,
    val isSuccessShown: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)
