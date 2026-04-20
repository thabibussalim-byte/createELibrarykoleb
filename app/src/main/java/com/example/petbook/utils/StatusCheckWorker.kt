package com.example.petbook.utils

import android.content.Context
import androidx.work.*
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.pref.PreferenceManager
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class StatusCheckWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefManager = PreferenceManager(applicationContext)
        val token = prefManager.getToken() ?: return Result.failure()
        val userId = prefManager.getUserId()
        if (userId <= 0) return Result.failure()
        
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val notificationHelper = NotificationHelper(applicationContext)

        // Ambil data lama untuk perbandingan (opsional, tapi di sini kita cek perubahan status)
        // Kita gunakan SharedPreferences untuk menyimpan status terakhir tiap transaksi ID agar tidak spam notifikasi
        val sharedPrefs = applicationContext.getSharedPreferences("transaction_status_prefs", Context.MODE_PRIVATE)

        try {
            val response = ApiConfig.getApiService().getHistoryByUser(formattedToken, userId).execute()
            if (response.isSuccessful) {
                val historyList = response.body()?.data ?: emptyList()
                
                var allFinished = true
                
                for (item in historyList) {
                    val lastStatus = sharedPrefs.getString("status_${item.id}", null)
                    
                    // Jika status berubah
                    if (lastStatus != null && lastStatus.lowercase() != item.status.lowercase()) {
                        when (item.status.lowercase()) {
                            "dipinjam", "disetujui" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Peminjaman Disetujui",
                                    "Peminjaman buku untuk ID #${item.id} telah disetujui oleh admin."
                                )
                            }
                            "ditolak" -> {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_CONFIRMATION,
                                    "Peminjaman Ditolak",
                                    "Mohon maaf, peminjaman buku untuk ID #${item.id} ditolak oleh admin."
                                )
                            }
                        }
                    }
                    
                    // Simpan status terbaru
                    sharedPrefs.edit().putString("status_${item.id}", item.status).apply()
                    
                    // Jika masih ada yang "pending" (menunggu konfirmasi), kita lanjut polling
                    if (item.status.lowercase() == "pending" || item.status.lowercase() == "menunggu") {
                        allFinished = false
                    }
                }

                // Jika semua transaksi sudah punya status final (disetujui/ditolak/kembali), hentikan worker ini
                if (allFinished && historyList.isNotEmpty()) {
                    // Berhenti secara otomatis tidak bisa dilakukan dari dalam doWork untuk PeriodicWork
                    // Namun untuk OneTimeWork yang di-chain, ini bisa diatur.
                    // Karena ini Periodic, ia akan terus jalan sampai di-cancel manual atau batas 3 hari.
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}