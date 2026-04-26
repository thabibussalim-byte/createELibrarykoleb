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

            // 1. Ambil data buku & denda terbaru untuk referensi
            val booksResponse = ApiConfig.getApiService().getBooks().execute()
            val bookList = booksResponse.body()?.data ?: emptyList()

            val finesResponse = ApiConfig.getApiService().getFines(formattedToken).execute()
            val fineList = finesResponse.body()?.data ?: emptyList()

            // 2. Ambil History terbaru (Gunakan getAllTransactions + filter)
            val response = ApiConfig.getApiService().getAllTransactions(formattedToken).execute()

            if (response.isSuccessful) {
                val allItems = response.body()?.data ?: emptyList()
                val historyList = allItems.filter { it.userId == userId }
                var hasActiveProcesses = false

                runBlocking {
                    for (item in historyList) {
                        val currentStatus = item.status.lowercase()

                        // Sinkronisasi Room
                        val existingEntity = repository.getHistoryById(item.id)
                        
                        // Update stok jika status berubah (Sudah pakai Token Admin di Repository)
                        if (existingEntity != null && existingEntity.status != item.status) {
                            repository.handleStockUpdate(item.bukuId, existingEntity.status, item.status)
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
                            isSuccessShown = existingEntity?.isSuccessShown ?: false
                        )
                        repository.insertHistory(listOf(entity))

                        // LOGIKA DENDA OTOMATIS (Sekarang pakai Token Admin via Repository)
                        if (currentStatus == "dipinjam" || currentStatus == "telat") {
                            checkAndCalculateFine(repository, item, fineList)
                            hasActiveProcesses = true
                        }

                        // Logika Notifikasi
                        val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                        if (lastStatus != null && lastStatus != currentStatus) {
                            val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            showStatusNotification(notificationHelper, currentStatus, bookTitle)
                        }
                        sharedPrefs.edit().putString("status_${item.id}", currentStatus).apply()

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

    private suspend fun checkAndCalculateFine(repository: PetbookRepository, history: com.example.petbook.data.api.model.HistoryDataItem, fineList: List<com.example.petbook.data.api.model.FineDataItem>) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val returnDateStr = history.tglKembali.take(10)
            if (returnDateStr.isEmpty()) return

            val returnDate = sdf.parse(returnDateStr)
            val currentDate = Date()

            if (returnDate != null && currentDate.after(returnDate)) {
                val diffInMillis = currentDate.time - returnDate.time
                val overdueDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                if (overdueDays > 0) {
                    val totalFineAmount = overdueDays * 2000 // Denda Rp 2000 per hari
                    val existingFine = fineList.find { it.transaksiId == history.id }
                    
                    // Panggil repository untuk create/update denda pakai Token Admin
                    repository.createOrUpdateFine(history.id, totalFineAmount, existingFine?.id)
                    
                    val title = ApiConfig.getApiService().getBooks().execute().body()?.data?.find { it.id == history.bukuId }?.judulBuku ?: "Buku"
                    NotificationHelper(applicationContext).showNotification(102, "Terlambat!", "Segera kembalikan buku \"$title\".")
                }
            }
        } catch (e: Exception) {
            Log.e("FineUpdate", "Gagal proses denda: ${e.message}")
        }
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
