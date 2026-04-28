package com.example.petbook.data.repository

import android.util.Log
import com.example.petbook.data.api.ApiService
import com.example.petbook.data.api.model.FineRequest
import com.example.petbook.data.api.model.LoginRequest
import com.example.petbook.data.local.entity.BookEntity
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.local.room.BookDao
import kotlinx.coroutines.flow.Flow
import retrofit2.awaitResponse

class PetbookRepository(
    private val apiService: ApiService,
    private val bookDao: BookDao
) {

    private var adminToken: String? = null

    private val adminCredentials = LoginRequest("admin", "admin123")

    private suspend fun getLatestAdminToken(): String? {
        try {
            val response = apiService.login(adminCredentials).awaitResponse()
            if (response.isSuccessful) {
                val newToken = response.body()?.data?.token
                if (newToken != null) {
                    adminToken = "Bearer $newToken"
                    return adminToken
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Gagal mengambil token admin: ${e.message}")
        }
        return adminToken
    }

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getHistoryByUserId(userId: Int): Flow<List<HistoryEntity>> = 
        bookDao.getHistoryByUserId(userId.toString())

    suspend fun getHistoryById(id: Int): HistoryEntity? = bookDao.getHistoryById(id)

    suspend fun insertHistory(history: List<HistoryEntity>) = bookDao.insertHistory(history)

    suspend fun refreshHistory(userId: Int) {
        val token = getLatestAdminToken() ?: return
        try {
            val response = apiService.getAllTransactions(token).awaitResponse()
            if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                val filteredItems = items.filter { it.userId == userId }
                
                for (item in filteredItems) {
                    val existing = bookDao.getHistoryById(item.id)
                    
                    if (existing != null && existing.status.lowercase() != item.status.lowercase()) {
                        handleStockUpdate(item.bukuId, existing.status, item.status)
                    }

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

    suspend fun handleStockUpdate(bookId: Int, oldStatus: String, newStatus: String) {
        val old = oldStatus.lowercase()
        val new = newStatus.lowercase()

        if (old == "pending" && new == "dipinjam") {
            updateBookStock(bookId, -1)
        } 
        else if ((old == "dipinjam" || old == "telat") && (new == "dikembalikan" || new == "selesai")) {
            updateBookStock(bookId, 1)
        }
    }

    private suspend fun updateBookStock(bookId: Int, change: Int) {
        val token = getLatestAdminToken() ?: return
        try {
            val bookResponse = apiService.getBooks().awaitResponse()
            if (bookResponse.isSuccessful) {
                val allBooks = bookResponse.body()?.data ?: emptyList()
                val currentBook = allBooks.find { it.id == bookId }
                
                if (currentBook != null) {
                    val newStock = currentBook.stok + change
                    
                    val requestBody = mutableMapOf<String, Any>()
                    requestBody["judul_buku"] = currentBook.judulBuku
                    requestBody["deskripsi"] = currentBook.deskripsi
                    requestBody["stok"] = newStock.toString()
                    requestBody["tgl_terbit"] = currentBook.tglTerbit
                    requestBody["genre_id"] = currentBook.genreId
                    requestBody["penulis_id"] = currentBook.penulisId
                    requestBody["penerbit_id"] = currentBook.penerbitId

                    val response = apiService.updateBook(token, bookId, requestBody).awaitResponse()
                    if (response.isSuccessful) {
                        val entity = BookEntity(
                            id = currentBook.id,
                            judulBuku = currentBook.judulBuku,
                            deskripsi = currentBook.deskripsi,
                            stok = newStock,
                            tanggalTerbit = currentBook.tglTerbit,
                            foto = currentBook.foto,
                            genreId = currentBook.genreId,
                            penulisId = currentBook.penulisId,
                            penerbitId = currentBook.penerbitId,
                            createdAt = currentBook.createdAt,
                            updatedAt = currentBook.updatedAt
                        )
                        bookDao.insertBooks(listOf(entity))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "EXCEPTION in updateBookStock: ${e.message}")
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
