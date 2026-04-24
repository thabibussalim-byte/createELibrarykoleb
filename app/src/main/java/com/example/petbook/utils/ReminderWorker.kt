package com.example.petbook.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.FineRequest
import com.example.petbook.data.pref.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefManager = PreferenceManager(applicationContext)
        val token = prefManager.getToken() ?: return Result.failure()
        val userId = prefManager.getUserId()
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val apiService = ApiConfig.getApiService()
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            // 1. MEMANGGIL STATUS TRANSAKSI DULUAN
            val historyResponse = apiService.getHistoryByUser(formattedToken, userId).execute()
            val historyList = historyResponse.body()?.data ?: emptyList()

            // Ambil data denda & buku untuk referensi
            val finesResponse = apiService.getFines(formattedToken).execute()
            val existingFines = finesResponse.body()?.data ?: emptyList()
            val bookList = apiService.getBooks().execute().body()?.data ?: emptyList()

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(
                Calendar.MILLISECOND,
                0
            )
            }.time

            for (item in historyList) {
                // JIKA STATUS MASIH DIPINJAM
                if (item.status.lowercase() == "dipinjam") {
                    val dueDate = sdf.parse(item.tglKembali.take(10))

                    // JIKA SUDAH LEWAT TANGGAL KEMBALI
                    if (dueDate != null && today.after(dueDate)) {
                        val diff = today.time - dueDate.time
                        val daysLate = (diff / (1000 * 60 * 60 * 24)).toInt()

                        if (daysLate > 0) {
                            // HITUNG DENDA: HARI 1 = 2000, HARI 2 = 4000, DST. MAX 100RB
                            val calculatedFine =
                                if (daysLate * 2000 > 100000) 100000 else daysLate * 2000

                            val existingFine = existingFines.find { it.transaksiId == item.id }
                            val fineRequest = FineRequest(
                                totalDenda = calculatedFine.toString(),
                                status = "belumdibayar",
                                transaksiId = item.id
                            )

                            if (existingFine == null) {
                                // HARI PERTAMA TELAT (Belum ada data denda di DB)
                                apiService.createFine(formattedToken, fineRequest).execute()
                            } else {
                                // HARI BERIKUTNYA (Sudah ada data denda, kita UPDATE nilainya)
                                val currentDendaInDb =
                                    existingFine.totalDenda.replace(Regex("[^0-9]"), "")
                                        .toIntOrNull() ?: 0

                                // Hanya update jika jumlah denda bertambah
                                if (currentDendaInDb < calculatedFine) {
                                    apiService.updateFine(
                                        formattedToken,
                                        existingFine.id,
                                        fineRequest
                                    ).execute()
                                }
                            }

                            // Kirim Notifikasi
                            val bookTitle =
                                bookList.find { it.id == item.bukuId }?.judulBuku ?: "Buku"
                            notificationHelper.showNotification(
                                NotificationHelper.NOTIFICATION_ID_REMINDER + item.id,
                                "Tagihan Denda: Rp $calculatedFine",
                                "Buku \"$bookTitle\" telat $daysLate hari. Segera kembalikan!"
                            )
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
