package com.example.petbook.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.di.Injection
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.data.repository.PetbookRepository
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

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

            val booksResponse = ApiConfig.getApiService().getBooks().execute()
            val bookList = booksResponse.body()?.data ?: emptyList()

            val finesResponse = ApiConfig.getApiService().getFines(formattedToken).execute()
            val fineList = finesResponse.body()?.data ?: emptyList()

            val response = ApiConfig.getApiService().getAllTransactions(formattedToken).execute()

            if (response.isSuccessful) {
                val allItems = response.body()?.data ?: emptyList()
                val historyList = allItems.filter { it.userId == userId }
                var hasActiveProcesses = false

                runBlocking {
                    for (item in historyList) {
                        val currentStatus = item.status.lowercase()
                        val existingEntity = repository.getHistoryById(item.id)

                        if (currentStatus == "dipinjam") {
                            val isLate = checkAndCalculateFine(repository, item, fineList)
                            if (isLate) {
                                val lastWasLate = sharedPrefs.getBoolean("is_late_${item.id}", false)
                                if (!lastWasLate) {
                                    val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                                    notificationHelper.showNotification(101, "Peringatan", "Buku \"$bookTitle\" sudah melewati batas waktu!")
                                    sharedPrefs.edit { putBoolean("is_late_${item.id}", true) }
                                }
                            }
                            hasActiveProcesses = true
                        }

                        if (existingEntity != null && item.tglKembali != existingEntity.tanggalPengembalian) {
                            val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            val lastNotifiedDate = sharedPrefs.getString("notified_tgl_kembali_${item.id}", "")
                            
                            if (lastNotifiedDate != item.tglKembali) {
                                notificationHelper.showNotification(
                                    103, 
                                    "Tanggal Diperbarui", 
                                    "Buku \"$bookTitle\" diperpanjang hingga ${item.tglKembali.take(10)}"
                                )
                                sharedPrefs.edit { putString("notified_tgl_kembali_${item.id}", item.tglKembali) }
                            }
                        }

                        if (existingEntity != null && existingEntity.status != currentStatus) {
                            repository.handleStockUpdate(item.bukuId, existingEntity.status, currentStatus)
                        }

                        val entity = HistoryEntity(
                            id = item.id,
                            status = item.status, // Tetap gunakan status asli dari API
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

                        val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                        if (lastStatus != null && lastStatus != currentStatus) {
                            val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            showStatusNotification(notificationHelper, currentStatus, bookTitle)
                        }
                        sharedPrefs.edit { putString("status_${item.id}", currentStatus) }

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

    private suspend fun checkAndCalculateFine(repository: PetbookRepository, history: com.example.petbook.data.api.model.HistoryDataItem, fineList: List<com.example.petbook.data.api.model.FineDataItem>): Boolean {
        var isLate = false
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val returnDateStr = history.tglKembali.take(10)
            if (returnDateStr.isEmpty()) return false

            val returnDate = sdf.parse(returnDateStr)
            val currentDate = Date()

            if (returnDate != null && currentDate.after(returnDate)) {
                isLate = true
                val diffInMillis = currentDate.time - returnDate.time
                val overdueDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                if (overdueDays > 0) {
                    val totalFineAmount = overdueDays * 2000
                    val existingFine = fineList.find { it.transaksiId == history.id }
                    repository.createOrUpdateFine(history.id, totalFineAmount, existingFine?.id)
                }
            }
        } catch (e: Exception) {
            Log.e("FineUpdate", "Gagal proses denda: ${e.message}")
        }
        return isLate
    }

    private fun showStatusNotification(helper: NotificationHelper, status: String, title: String) {
        when (status) {
            "dipinjam" -> helper.showNotification(101, "Peminjaman Disetujui", "Buku \"$title\" siap diambil.")
            "dikembalikan" -> helper.showNotification(101, "Sukses", "Buku \"$title\" telah dikembalikan.")
        }
    }

    private fun handleWorkerLifetime(active: Boolean) {
        val startTime = inputData.getLong("start_time", System.currentTimeMillis())
        val isTimedOut = System.currentTimeMillis() - startTime > TimeUnit.DAYS.toMillis(2) 
        if (!active || isTimedOut) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("BorrowStatusCheck")
        }
    }
}
