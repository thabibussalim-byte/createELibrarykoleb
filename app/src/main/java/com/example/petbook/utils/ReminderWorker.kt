package com.example.petbook.utils

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.di.Injection
import com.example.petbook.data.local.entity.HistoryEntity
import com.example.petbook.data.pref.PreferenceManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val notifPrefs = applicationContext.getSharedPreferences("notif_tracking", Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val workerPrefs = applicationContext.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        val startTime = workerPrefs.getLong("start_time", 0L)
        val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L

        if (startTime != 0L && (System.currentTimeMillis() - startTime) > twoDaysInMillis) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("PetbookReminderWork")
            return Result.success()
        }

        val prefManager = PreferenceManager(applicationContext)
        val token = prefManager.getToken() ?: return Result.failure()
        val userId = prefManager.getUserId()
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val apiService = ApiConfig.getApiService()
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            val repository = Injection.provideRepository(applicationContext)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = sdf.format(Date())

            val booksResponse = apiService.getBooks().execute()
            val bookList = booksResponse.body()?.data ?: emptyList()
            val newestBook = bookList.maxByOrNull { it.id }
            val lastSeenBookId = notifPrefs.getInt("last_book_id", 0)
            
            if (newestBook != null && newestBook.id > lastSeenBookId) {
                notificationHelper.showNotification(
                    999, 
                    "Buku Baru!", 
                    "Baru saja tersedia: ${newestBook.judulBuku}",
                    target = NotificationHelper.TARGET_CATALOG,
                    imageUrl = newestBook.foto
                )
                notifPrefs.edit { putInt("last_book_id", newestBook.id) }
            }

            val allHistoryResponse = apiService.getAllTransactions(formattedToken).execute()
            val allItems = allHistoryResponse.body()?.data ?: emptyList()
            val historyList = allItems.filter { it.userId == userId }

            val finesResponse = apiService.getFines(formattedToken).execute()
            val existingFines = finesResponse.body()?.data ?: emptyList()

            runBlocking {
                for (item in historyList) {
                    val existingEntity = repository.getHistoryById(item.id)
                    val bookTitle = bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                    
                    if (existingEntity != null && existingEntity.status != item.status) {
                        repository.handleStockUpdate(item.bukuId, existingEntity.status, item.status)

                        val msg = when (item.status.lowercase()) {
                            "dipinjam" -> "Pinjaman buku \"$bookTitle\" disetujui!"
                            "dikembalikan", "selesai" -> "Buku \"$bookTitle\" berhasil dikembalikan."
                            else -> null
                        }
                        if (msg != null) {
                            notificationHelper.showNotification(
                                100 + item.id, 
                                "Update Status", 
                                msg,
                                target = NotificationHelper.TARGET_HISTORY
                            )
                        }
                    }

                    repository.insertHistory(listOf(HistoryEntity(
                        id = item.id, status = item.status, tanggalPinjam = item.tglPinjam,
                        tanggalPengembalian = item.tglKembali, keterangan = item.keterangan,
                        bukuId = item.bukuId, userId = item.userId, denda = item.denda,
                        isSuccessShown = existingEntity?.isSuccessShown ?: false
                    )))

                    if (item.status.lowercase() == "dipinjam") {
                        val dueDate = try { sdf.parse(item.tglKembali.take(10)) } catch (e: Exception) { null }
                        if (dueDate != null && Date().after(dueDate)) {
                            val daysLate = ((Date().time - dueDate.time) / (1000 * 60 * 60 * 24)).toInt()
                            val lastNotifDate = notifPrefs.getString("late_notif_${item.id}", "")
                            
                            if (daysLate > 0 && lastNotifDate != todayDate) {
                                notificationHelper.showNotification(
                                    200 + item.id, 
                                    "Buku Terlambat!", 
                                    "Buku \"$bookTitle\" telat $daysLate hari. Segera kembalikan!",
                                    target = NotificationHelper.TARGET_HISTORY
                                )
                                notifPrefs.edit { putString("late_notif_${item.id}", todayDate) }
                            }
                        }
                    }

                    val fine = existingFines.find { it.transaksiId == item.id }
                    val savedFineStatus = notifPrefs.getString("fine_status_${item.id}", "")
                    
                    if (fine != null && savedFineStatus == "belumdibayar" && fine.status == "dibayar") {
                        notificationHelper.showNotification(
                            300 + item.id, 
                            "Pembayaran Lunas", 
                            "Denda buku \"$bookTitle\" telah dibayar.",
                            target = NotificationHelper.TARGET_HISTORY
                        )
                    }
                    if (fine != null) notifPrefs.edit {
                        putString(
                            "fine_status_${item.id}",
                            fine.status
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error: ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }
}
