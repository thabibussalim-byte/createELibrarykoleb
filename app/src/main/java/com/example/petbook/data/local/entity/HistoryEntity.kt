package com.example.petbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "histories") data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val status: String,
    val tanggalPinjam: String,
    val tanggalPengembalian: String?,
    val bukuId: String,
    val userId: String,
    val createdAt: String?,
    val updatedAt: String?
)