package com.example.petbook.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petbook.data.local.entity.BookEntity
import com.example.petbook.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("SELECT * FROM histories")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM histories WHERE userId = :userId")
    fun getHistoryByUserId(userId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM histories WHERE id = :id")
    suspend fun getHistoryById(id: Int): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: List<HistoryEntity>)

    @Query("UPDATE histories SET isSuccessShown = :isShown WHERE id = :id")
    suspend fun updateSuccessStatus(id: Int, isShown: Boolean)

    @Query("DELETE FROM histories")
    suspend fun deleteAllHistory()
}
