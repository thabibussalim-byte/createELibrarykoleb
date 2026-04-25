package com.example.petbook.data.repository

import android.util.Log
import com.example.petbook.data.api.ApiService
import com.example.petbook.data.api.model.BorrowRequest
import com.example.petbook.data.api.model.FineRequest
import com.example.petbook.data.local.entity.BookEntity
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.local.room.BookDao
import kotlinx.coroutines.flow.Flow
import retrofit2.awaitResponse

class PetbookRepository(
    private val apiService: ApiService,
    private val bookDao: BookDao
) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getHistoryByUserId(userId: Int): Flow<List<HistoryEntity>> = 
        bookDao.getHistoryByUserId(userId.toString())

    suspend fun getHistoryById(id: Int): HistoryEntity? = bookDao.getHistoryById(id)

    suspend fun updateSuccessStatus(id: Int, isShown: Boolean) = bookDao.updateSuccessStatus(id, isShown)

    suspend fun insertHistory(history: List<HistoryEntity>) = bookDao.insertHistory(history)

    suspend fun updateTransaction(token: String, id: Int, request: BorrowRequest) =
        apiService.updateTransaction("Bearer $token", id, request).awaitResponse()

    suspend fun createFine(token: String, request: FineRequest) =
        apiService.createFine("Bearer $token", request).awaitResponse()

    suspend fun updateBook(token: String, id: Int, request: Map<String, Any>) =
        apiService.updateBook("Bearer $token", id, request).awaitResponse()

    suspend fun refreshHistory(token: String, userId: Int) {
        try {
            val response = apiService.getAllTransactions("Bearer $token").awaitResponse()
            if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                val filteredItems = items.filter { it.userId == userId }
                
                for (item in filteredItems) {
                    val existing = bookDao.getHistoryById(item.id)
                    val entity = HistoryEntity(
                        id = item.id,
                        status = item.status,
                        tanggalPinjam = item.tglPinjam,
                        tanggalPengembalian = item.tglKembali,
                        keterangan = item.keterangan,
                        bukuId = item.bukuId,
                        userId = item.userId,
                        denda = item.denda,
                        isSuccessShown = existing?.isSuccessShown ?: false
                    )
                    bookDao.insertHistory(listOf(entity))
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing history: ${e.message}")
        }
    }

    suspend fun refreshBooks() {
        try {
            val response = apiService.getBooks().awaitResponse()
            if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                val entities = items.map {
                    BookEntity(
                        id = it.id,
                        judulBuku = it.judulBuku,
                        deskripsi = it.deskripsi,
                        stok = it.stok,
                        tanggalTerbit = it.tglTerbit,
                        foto = it.foto,
                        genreId = it.genreId,
                        penulisId = it.penulisId,
                        penerbitId = it.penerbitId,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
                bookDao.insertBooks(entities)
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing books: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: PetbookRepository? = null
        fun getInstance(apiService: ApiService, bookDao: BookDao): PetbookRepository =
            instance ?: synchronized(this) {
                instance ?: PetbookRepository(apiService, bookDao)
            }.also { instance = it }
    }
}
