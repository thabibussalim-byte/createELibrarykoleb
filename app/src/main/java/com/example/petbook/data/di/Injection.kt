package com.example.petbook.data.di

import android.content.Context
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.local.room.AppDatabase
import com.example.petbook.data.repository.PetbookRepository

object Injection {
    fun provideRepository(context: Context): PetbookRepository {
        val database = AppDatabase.getInstance(context)
        val apiService = ApiConfig.getApiService()
        return PetbookRepository.getInstance(apiService, database.bookDao())
    }
}