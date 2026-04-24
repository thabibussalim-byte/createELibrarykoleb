package com.example.petbook.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.FineRequest
import com.example.petbook.data.di.Injection
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.pref.PreferenceManager
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

            // 2. Ambil History terbaru (untuk cek perpanjangan tanggal & status)
            val response = ApiConfig.getApiService().getHistoryByUser(formattedToken, userId).execute()

            if (response.isSuccessful) {
                val historyList = response.body()?.data ?: emptyList()
                var hasActiveProcesses = false

                runBlocking {
                    for (item in historyList) {
                        val currentStatus = item.status.lowercase()

                        // --- SINKRONISASI ROOM (Offline-First) ---
                        val existingEntity = repository.getHistoryById(item.id)
                        val entity = HistoryEntity(
                            id = item.id,
                            status = item.status,
                            tanggalPinjam = item.tglPinjam,
                            tanggalPengembalian = item.tglKembali, // Mendukung Perpanjangan Tanggal
                            keterangan = item.keterangan,
                            bukuId = item.bukuId,
                            userId = item.userId,
                            denda = item.denda,
                            isSuccessShown = existingEntity?.isSuccessShown ?: false
                        )
                        repository.insertHistory(listOf(entity))

                        // --- LOGIKA UPDATE DENDA OTOMATIS (Jika Terlambat) ---
                        if (currentStatus == "dipinjam") {
                            checkAndCalculateFine(formattedToken, item, fineList)
                            hasActiveProcesses = true
                        }

                        // --- LOGIKA NOTIFIKASI PERUBAHAN STATUS ---
                        val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                        if (lastStatus != null && lastStatus != currentStatus) {
                            val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            showStatusNotification(notificationHelper, currentStatus, bookTitle)
                        }
                        sharedPrefs.edit().putString("status_${item.id}", currentStatus).apply()

                        if (currentStatus == "pending") hasActiveProcesses = true
                    }
                }

                // Pengaturan WorkManager (Auto-Stop jika sudah selesai)
                handleWorkerLifetime(hasActiveProcesses)
            }
        } catch (e: Exception) {
            Log.e("StatusCheckWorker", "Error: ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }

    private fun checkAndCalculateFine(token: String, history: com.example.petbook.data.api.model.HistoryDataItem, fineList: List<com.example.petbook.data.api.model.FineDataItem>) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val returnDateStr = history.tglKembali
            if (returnDateStr.isEmpty()) return

            val returnDate = sdf.parse(returnDateStr)
            val currentDate = Date()

            if (currentDate.after(returnDate)) {
                val diffInMillis = currentDate.time - returnDate.time
                val overdueDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                if (overdueDays > 0) {
                    val totalFineAmount = overdueDays * 2000

                    // Cari ID Denda berdasarkan ID Transaksi
                    val existingFine = fineList.find { it.transaksiId == history.id }

                    if (existingFine != null) {
                        // Jika denda sudah ada di server, lakukan PATCH (Update)
                        val request = FineRequest(totalFineAmount.toString(), "belumdibayar", history.id)
                        ApiConfig.getApiService().updateFine(token, existingFine.id, request).execute()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FineUpdate", "Gagal hitung denda: ${e.message}")
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
        val isTimedOut = System.currentTimeMillis() - startTime > TimeUnit.DAYS.toMillis(1)
        if (!active || isTimedOut) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("BorrowStatusCheck")
        }
    }
}