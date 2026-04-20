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
        val id = prefManager.getUserId()
        val formattedToken = (if (token.startsWith("Bearer ")) token else "Bearer $token")
        
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            val response = ApiConfig.getApiService().getHistoryByUser(formattedToken,id).execute()
            if (response.isSuccessful) {
                val historyList = response.body()?.data ?: emptyList()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = Calendar.getInstance().time

                for (item in historyList) {
                    if (item.status.lowercase() == "dipinjam") {
                        val dueDate = sdf.parse(item.tglKembali)
                        if (dueDate != null) {
                            val diff = dueDate.time - today.time
                            val daysLeft = diff / (1000 * 60 * 60 * 24)

                            if (daysLeft in 0..1) {
                                notificationHelper.showNotification(
                                    NotificationHelper.NOTIFICATION_ID_REMINDER,
                                    "Pengingat Jatuh Tempo",
                                    "Buku yang Anda pinjam akan segera jatuh tempo pada ${item.tglKembali}. Harap segera kembalikan."
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}