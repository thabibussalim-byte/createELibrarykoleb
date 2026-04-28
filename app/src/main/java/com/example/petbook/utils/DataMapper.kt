package com.example.petbook.utils

import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.local.entity.BookEntity
import com.example.petbook.data.local.entity.HistoryEntity

object DataMapper {
    fun BookEntity.toBookItem(): BookItem {
        return BookItem(
            id = id,
            judulBuku = judulBuku,
            deskripsi = deskripsi ?: "",
            stok = stok,
            tglTerbit = tanggalTerbit ?: "",
            foto = foto ?: "",
            genreId = genreId ?: 0,
            penulisId = penulisId ?: 0,
            penerbitId = penerbitId ?: 0,
            createdAt = createdAt ?: "",
            updatedAt = updatedAt ?: ""
        )
    }

    fun HistoryEntity.toDataItem(): HistoryDataItem {
        return HistoryDataItem(
            id = id,
            status = status,
            tglPinjam = tanggalPinjam,
            tglKembali = tanggalPengembalian ?: "",
            keterangan = keterangan,
            bukuId = bukuId,
            userId = userId,
            denda = denda,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
