package com.example.petbook.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.pref.PreferenceManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

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
                val allFinesList = fineResponse.body()?.data ?: emptyList()
                

                val userHistory = if (historyResponse.isSuccessful) historyResponse.body()?.data ?: emptyList() else emptyList()
                val userTransactionIds = userHistory.map { it.id }.toSet()
                
                val userFines = allFinesList.filter { userTransactionIds.contains(it.transaksiId) }
                
                for (fine in userFines) {
                    val lastStatus = sharedPrefs.getString("fine_status_${fine.id}", null)
                    val currentStatus = fine.status.lowercase()
                    
                    val cleanAmount = fine.totalDenda.replace(Regex("[^0-9]"), "")
                    val amountInt = cleanAmount.toIntOrNull() ?: 0
                    val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    val formattedAmount = formatRupiah.format(amountInt).replace(",00", "").replace("Rp", "Rp: ")

                    if (currentStatus.contains("belumdibayar")) {
                        notificationHelper.showNotification(
                            NotificationHelper.NOTIFICATION_ID_REMINDER + fine.id,
                            "Tagihan Denda Aktif",
                            "Anda memiliki denda sebesar $formattedAmount yang belum dibayar."
                        )
                    } else if (lastStatus != null && lastStatus.contains("belumdibayar") && (currentStatus.contains("lunas") || currentStatus.contains("dibayar"))) {
                        notificationHelper.showNotification(
                            NotificationHelper.NOTIFICATION_ID_REMINDER + fine.id,
                            "Pembayaran Berhasil",
                            "Terima kasih, denda $formattedAmount untuk transaksi telah berhasil dibayar."
                        )
                    }
                    
                    sharedPrefs.edit { putString("fine_status_${fine.id}", currentStatus) }
                }
            }

        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
