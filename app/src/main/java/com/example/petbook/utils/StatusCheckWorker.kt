package com.example.petbook.utils

import android.content.Context
import androidx.work.*
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.pref.PreferenceManager
import java.util.concurrent.TimeUnit

class StatusCheckWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefManager = PreferenceManager(applicationContext)
        val token = prefManager.getToken() ?: return Result.failure()
        val userId = prefManager.getUserId()
        if (userId <= 0) return Result.failure()
        
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val notificationHelper = NotificationHelper(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences("transaction_status_prefs", Context.MODE_PRIVATE)

        try {
            // Ambil data buku untuk mendapatkan judul
            val booksResponse = ApiConfig.getApiService().getBooks().execute()
            val bookList = if (booksResponse.isSuccessful) booksResponse.body()?.data ?: emptyList() else emptyList()

            val response = ApiConfig.getApiService().getHistoryByUser(formattedToken, userId).execute()
            if (response.isSuccessful) {
                val historyList = response.body()?.data ?: emptyList()
                
                var hasActiveProcesses = false
                
                for (item in historyList) {
                    val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                    val currentStatus = item.status.lowercase()
                    
                    // Cari judul buku berdasarkan bukuId
                    val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku (ID: ${item.bukuId})"
                    
                    if (lastStatus != null && lastStatus.lowercase() != currentStatus) {
                        when (currentStatus) {
                            "dipinjam" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Peminjaman Disetujui",
                                    "Peminjaman buku \"$bookTitle\" telah disetujui. Silakan ambil buku di perpustakaan."
                                )
                            }
                            "dikembalikan"-> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Buku Berhasil Dikembalikan",
                                    "Terima kasih, buku \"$bookTitle\" telah resmi dikembalikan. Tanggung jawab peminjaman selesai."
                                )
                            }
                        }
                    }
                    
                    // Simpan status terbaru
                    sharedPrefs.edit().putString("status_${item.id}", currentStatus).apply()
                    
                    if (currentStatus == "pending" || currentStatus == "dipinjam") {
                        hasActiveProcesses = true
                    }
                }

                // Cek Batas Waktu 1 Hari
                val startTime = inputData.getLong("start_time", System.currentTimeMillis())
                val isTimedOut = System.currentTimeMillis() - startTime > TimeUnit.DAYS.toMillis(1)

                if (!hasActiveProcesses || isTimedOut) {
                    WorkManager.getInstance(applicationContext).cancelUniqueWork("BorrowStatusCheck")
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
