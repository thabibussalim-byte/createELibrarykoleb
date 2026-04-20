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
            val response = ApiConfig.getApiService().getHistoryByUser(formattedToken, userId).execute()
            if (response.isSuccessful) {
                val historyList = response.body()?.data ?: emptyList()
                
                var hasActiveProcesses = false
                
                for (item in historyList) {
                    val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                    val currentStatus = item.status.lowercase()
                    
                    if (lastStatus != null && lastStatus.lowercase() != currentStatus) {
                        when (currentStatus) {
                            "dipinjam", "disetujui" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Peminjaman Disetujui",
                                    "Peminjaman buku ID #${item.id} telah disetujui. Silakan ambil buku di perpustakaan."
                                )
                            }
                            "ditolak" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Peminjaman Ditolak",
                                    "Mohon maaf, permintaan peminjaman buku ID #${item.id} ditolak oleh admin."
                                )
                            }
                            "dikembalikan", "kembali", "selesai" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Buku Berhasil Dikembalikan",
                                    "Terima kasih, buku ID #${item.id} telah resmi dikembalikan. Tanggung jawab peminjaman selesai."
                                )
                            }
                        }
                    }
                    
                    // Simpan status terbaru
                    sharedPrefs.edit().putString("status_${item.id}", currentStatus).apply()
                    
                    // Cek apakah masih ada proses yang membutuhkan polling (pending/menunggu/dipinjam)
                    // Kita anggap "dipinjam" juga aktif karena kita menunggu status berubah jadi "kembali"
                    if (currentStatus == "pending" || currentStatus == "menunggu" || currentStatus == "dipinjam") {
                        hasActiveProcesses = true
                    }
                }

                // Cek Batas Waktu 3 Hari dari InputData (hanya untuk pengingat timeout)
                val startTime = inputData.getLong("start_time", System.currentTimeMillis())
                val isTimedOut = System.currentTimeMillis() - startTime > TimeUnit.DAYS.toMillis(3)

                // Hentikan worker jika tidak ada proses aktif lagi atau sudah lewat 3 hari
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