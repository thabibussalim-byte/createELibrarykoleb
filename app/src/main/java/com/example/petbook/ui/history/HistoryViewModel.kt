package com.example.petbook.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petbook.data.local.entity.BookEntity
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.repository.PetbookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: PetbookRepository) : ViewModel() {

    fun getAllBooks(): Flow<List<BookEntity>> =
        repository.getAllBooks()

    fun refreshBooks() {
        viewModelScope.launch {
            repository.refreshBooks()
        }
    }

    fun getHistoryByUserId(userId: Int): Flow<List<HistoryEntity>> =
        repository.getHistoryByUserId(userId)

    fun refreshHistory(userId: Int) {
        viewModelScope.launch {
            repository.refreshHistory(userId)
        }
    }
}
