package com.example.petbook.ui.history

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentHistoryBinding
import com.example.petbook.utils.NotificationHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper
    
    private var allHistory: List<HistoryDataItem> = emptyList()
    private var allBooks: List<BookItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()
    private var allFines: List<FineDataItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        applyChipStyles()
        setupFilters()
        loadRequiredData()
    }

    private fun applyChipStyles() {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val backgroundColors = intArrayOf(
            Color.parseColor("#DBEAFE"),
            Color.parseColor("#F1F5F9")
        )
        val textColors = intArrayOf(
            Color.parseColor("#1E40AF"),
            Color.parseColor("#64748B")
        )

        val colorStateListBg = ColorStateList(states, backgroundColors)
        val colorStateListText = ColorStateList(states, textColors)

        val chips = listOf(
            binding.chipAll,
            binding.chipPending,
            binding.chipDipinjam,
            binding.chipDikembalikan
        )

        for (chip in chips) {
            chip.chipBackgroundColor = colorStateListBg
            chip.setTextColor(colorStateListText)
            chip.chipStrokeWidth = 0f
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList(), emptyList(), emptyList(), emptyList()) { historyItem ->
            if (!isAdded) return@HistoryAdapter

            val book = allBooks.find { it.id == historyItem.bukuId }
            val author = allAuthors.find { it.id == book?.penulisId }?.namaPenulis ?: "Penulis Anonim"
            val fine = allFines.find { it.transaksiId == historyItem.id }

            if (book != null) {
                val bundle = Bundle().apply {
                    putParcelable("history", historyItem)
                    putParcelable("book", book)
                    putString("author", author)
                    putParcelable("fine", fine)
                }
                
                try {
                    val currentDest = findNavController().currentDestination?.id
                    if (currentDest == R.id.historyFragment) {
                        findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment, bundle)
                    }
                } catch (e: Exception) {
                    Log.e("History", "Navigasi Error: ${e.message}")
                }
            }
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun loadRequiredData() {
        binding.progressBarHistory.visibility = View.VISIBLE
        
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    loadBooks()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                if (_binding != null) loadBooks()
            }
        })
    }

    private fun loadBooks() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allBooks = response.body()?.data ?: emptyList()
                    loadFines()
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                if (_binding != null) loadFines()
            }
        })
    }

    private fun loadFines() {
        val token = prefManager.getToken()
        if (token.isNullOrEmpty()) {
            loadHistory()
            return
        }

        val authHeader = "Bearer $token"
        ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
            override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allFines = response.body()?.data ?: emptyList()
                }
                loadHistory()
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                if (_binding != null) loadHistory()
            }
        })
    }

    private fun loadHistory() {
        val token = prefManager.getToken()
        val currentUserId = prefManager.getUserId()
        
        if (token.isNullOrEmpty()) {
            binding.progressBarHistory.visibility = View.GONE
            return
        }

        val authHeader = "Bearer $token"
        
        ApiConfig.getApiService().getHistoryByUser(authHeader, currentUserId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    binding.progressBarHistory.visibility = View.GONE
                    if (response.isSuccessful) {
                        allHistory = response.body()?.data ?: emptyList()
                        checkStatusChangesAndNotify(allHistory)
                        historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines)
                    } else {
                        loadAllTransactionsFallback(authHeader, currentUserId)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) loadAllTransactionsFallback(authHeader, currentUserId)
            }
        })
    }

    private fun loadAllTransactionsFallback(authHeader: String, userId: Int) {
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    binding.progressBarHistory.visibility = View.GONE
                    if (response.isSuccessful) {
                        val rawData = response.body()?.data ?: emptyList()
                        allHistory = rawData.filter { it.userId == userId }
                        checkStatusChangesAndNotify(allHistory)
                        historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarHistory.visibility = View.GONE
            }
        })
    }

    private fun checkStatusChangesAndNotify(historyList: List<HistoryDataItem>) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Sesuaikan timezone server

        for (item in historyList) {
            val lastStatus = prefManager.getLastTransactionStatus(item.id)
            val currentStatus = item.status.lowercase()
            val book = allBooks.find { it.id == item.bukuId }
            val bookTitle = book?.judulBuku ?: "Buku"

            // 1. Cek jika baru meminjam (status pending/dipinjam & baru dibuat < 10 menit)
            try {
                val tglPinjamDate = dateFormat.parse(item.tglPinjam)
                val diffMinutes = if (tglPinjamDate != null) (now - tglPinjamDate.time) / (1000 * 60) else Long.MAX_VALUE
                
                if (diffMinutes < 10 && !prefManager.isStatusNotified(item.id, "new_loan")) {
                    notificationHelper.showNotification(
                        item.id,
                        "Peminjaman Berhasil",
                        "Anda telah meminjam buku $bookTitle. Harap dikembalikan tepat waktu."
                    )
                    prefManager.setStatusNotified(item.id, "new_loan")
                }
            } catch (e: Exception) {
                Log.e("History", "Error parsing date: ${e.message}")
            }

            // 2. Cek perubahan dari pending ke dipinjam (kurang dari 10 menit sejak perubahan/load)
            if (lastStatus == "pending" && currentStatus == "dipinjam") {
                if (!prefManager.isStatusNotified(item.id, "dipinjam")) {
                    notificationHelper.showNotification(
                        item.id + 1000, // ID unik
                        "Status Diperbarui",
                        "Permintaan pinjam buku $bookTitle telah disetujui."
                    )
                    prefManager.setStatusNotified(item.id, "dipinjam")
                }
            }

            // 3. Cek perubahan ke dikembalikan & arahkan ke fragment sukses
            if (currentStatus == "dikembalikan" && lastStatus != "dikembalikan") {
                if (!prefManager.isSuccessScreenShown(item.id)) {
                    prefManager.setSuccessScreenShown(item.id)
                    val bundle = Bundle().apply {
                        putString("book_title", bookTitle)
                    }
                    findNavController().navigate(R.id.successReturnFragment, bundle)
                }
            }

            // Simpan status terakhir
            prefManager.saveLastTransactionStatus(item.id, currentStatus)
        }
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val filteredList = when (checkedIds.firstOrNull()) {
                R.id.chip_pending -> allHistory.filter { it.status.lowercase() == "pending" }
                R.id.chip_dipinjam -> allHistory.filter { it.status.lowercase() == "dipinjam" }
                R.id.chip_dikembalikan -> allHistory.filter { it.status.lowercase() == "dikembalikan" }
                else -> allHistory
            }
            historyAdapter.updateData(filteredList, allBooks, allAuthors, allFines)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
