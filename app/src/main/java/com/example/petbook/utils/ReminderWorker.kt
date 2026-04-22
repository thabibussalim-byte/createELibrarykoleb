package com.example.petbook.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.pref.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefManager = PreferenceManager(applicationContext)
        val token = prefManager.getToken() ?: return Result.failure()
        val userId = prefManager.getUserId()
        if (userId <= 0) return Result.failure()
        
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val notificationHelper = NotificationHelper(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences("fine_status_prefs", Context.MODE_PRIVATE)

        try {
            val booksResponse = ApiConfig.getApiService().getBooks().execute()
            val bookList = if (booksResponse.isSuccessful) booksResponse.body()?.data ?: emptyList() else emptyList()
            // 1. CEK JATUH TEMPO & TERLAMBAT
            val historyResponse = ApiConfig.getApiService().getHistoryByUser(formattedToken, userId).execute()
            if (historyResponse.isSuccessful) {
                val historyList = historyResponse.body()?.data ?: emptyList()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val today = calendar.time

                for (item in historyList) {
                    if (item.status.lowercase() == "dipinjam") {
                        val dueDateParsed = sdf.parse(item.tglKembali)
                        if (dueDateParsed != null) {
                            val dueDateCalendar = Calendar.getInstance()
                            dueDateCalendar.time = dueDateParsed
                            dueDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
                            dueDateCalendar.set(Calendar.MINUTE, 0)
                            dueDateCalendar.set(Calendar.SECOND, 0)
                            dueDateCalendar.set(Calendar.MILLISECOND, 0)
                            val dueDate = dueDateCalendar.time

                            val diff = dueDate.time - today.time
                            val daysLeft = diff / (1000 * 60 * 60 * 24)
                            val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku (ID: ${item.bukuId})"

                            if (daysLeft in 0..1) {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_REMINDER,
                                    "Pengingat Jatuh Tempo",
                                    "Buku #\"$bookTitle\" jatuh tempo pada ${item.tglKembali}. Harap segera kembalikan."
                                )
                            } else if (daysLeft < 0) {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_REMINDER,
                                    "Peringatan Terlambat!",
                                    "Peminjaman buku #\"$bookTitle\" sudah terlambat ${-daysLeft} hari. Segera kembalikan!"
                                )
                            }
                        }
                    }
                }
            }

            // 2. CEK STATUS DENDA
            val fineResponse = ApiConfig.getApiService().getFines(formattedToken).execute()
            if (fineResponse.isSuccessful) {
                val finesList = fineResponse.body()?.data ?: emptyList()
                
                for (fine in finesList) {
                    val lastStatus = sharedPrefs.getString("fine_status_${fine.id}", null)
                    val currentStatus = fine.status.lowercase()

                    // Jika status "belum dibayar" atau "belum_lunas" (tergantung string API)
                    if (currentStatus.contains("belumdibayar")) {
                        notificationHelper.showNotification(
                            NotificationHelper.NOTIFICATION_ID_REMINDER,
                            "Tagihan Denda Aktif",
                            "Anda memiliki denda sebesar Rp ${fine.totalDenda} yang belum dibayar."
                        )
                    } else if (lastStatus != null && lastStatus.contains("belumdibayar") && currentStatus.contains("lunas") || currentStatus.contains("dibayar")) {
                        // Notifikasi jika denda baru saja dibayar
                        notificationHelper.showNotification(
                            NotificationHelper.NOTIFICATION_ID_REMINDER,
                            "Pembayaran Berhasil",
                            "Terima kasih, denda #${fine.totalDenda} untuk transaksi telah berhasil dibayar."
                        )
                    }
                    
                    // Simpan status terbaru
                    sharedPrefs.edit().putString("fine_status_${fine.id}", currentStatus).apply()
                }
            }

        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}