package com.example.petbook.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.di.Injection
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.pref.PreferenceManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import com.example.petbook.data.api.model.HistoryDataItem
import kotlinx.coroutines.flow.firstOrNull

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
            val repository = Injection.provideRepository(applicationContext)

            val response = ApiConfig.getApiService().getAllTransactions(formattedToken).execute()

            if (response.isSuccessful) {
                val allItems = response.body()?.data ?: emptyList()
                val historyList = allItems.filter { it.userId == userId }
                var hasActiveProcesses = false

                runBlocking {
                    val localBooks = repository.getAllBooks().firstOrNull() ?: emptyList()

                    for (item in historyList) {
                        val currentStatus = item.status.lowercase()
                        val existingEntity = repository.getHistoryById(item.id)

                        if (currentStatus == "dipinjam") {
                            val isLate = checkIfLate(item)
                            if (isLate) {
                                val lastWasLate = sharedPrefs.getBoolean("is_late_${item.id}", false)
                                if (!lastWasLate) {
                                    val bookTitle = localBooks.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                                    notificationHelper.showNotification(101, "Peringatan", "Buku \"$bookTitle\" sudah melewati batas waktu!")
                                    sharedPrefs.edit { putBoolean("is_late_${item.id}", true) }
                                }
                            }
                            hasActiveProcesses = true
                        }

                        val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                        if (lastStatus != null && lastStatus != currentStatus) {
                            val bookTitle = localBooks.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            showStatusNotification(notificationHelper, currentStatus, bookTitle)
                        }
                        sharedPrefs.edit { putString("status_${item.id}", currentStatus) }

                        val entity = HistoryEntity(
                            id = item.id,
                            status = item.status,
                            tanggalPinjam = item.tglPinjam,
                            tanggalPengembalian = item.tglKembali,
                            keterangan = item.keterangan,
                            bukuId = item.bukuId,
                            userId = item.userId,
                            denda = item.denda,
                            isSuccessShown = existingEntity?.isSuccessShown ?: false,
                            createdAt = item.createdAt,
                            updatedAt = item.updatedAt
                        )
                        repository.insertHistory(listOf(entity))

                        if (currentStatus == "pending") hasActiveProcesses = true
                    }
                }
                
                handleWorkerLifetime(hasActiveProcesses)
            }
        } catch (e: Exception) {
            Log.e("StatusCheckWorker", "Error: ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }

    private fun checkIfLate(history: HistoryDataItem): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val returnDateStr = history.tglKembali.take(10)
            if (returnDateStr.isEmpty()) return false

            val returnDate = sdf.parse(returnDateStr)
            val currentDate = Date()

            returnDate != null && currentDate.after(returnDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun showStatusNotification(helper: NotificationHelper, status: String, title: String) {
        when (status) {
            "dipinjam" -> helper.showNotification(101, "Peminjaman Disetujui", "Buku \"$title\" siap diambil.")
            "dikembalikan" -> helper.showNotification(102, "Sukses", "Buku \"$title\" telah dikembalikan.")
            "selesai" -> helper.showNotification(102, "Selesai", "Transaksi buku \"$title\" telah selesai.")
        }
    }

    private fun handleWorkerLifetime(active: Boolean) {
        val startTime = inputData.getLong("start_time", System.currentTimeMillis())
        val isTimedOut = System.currentTimeMillis() - startTime > TimeUnit.DAYS.toMillis(1)
        
        if (!active || isTimedOut) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("BorrowStatusCheck")
            Log.d("StatusCheckWorker", "Worker Stopped: active=$active, timeout=$isTimedOut")
        }
    }
}
