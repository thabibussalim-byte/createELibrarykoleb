package com.example.petbook.ui.history

import android.content.res.ColorStateList
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
import androidx.core.graphics.toColorInt

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper
    
    private var allHistory: List<HistoryDataItem> = emptyList()
    private var allBooks: List<BookItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()

    private var allPublishers: List<PublisherItem> = emptyList()
    private var allGenres: List<GenreItem> = emptyList()

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

    private fun loadBooks() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allBooks = response.body()?.data ?: emptyList()
                    loadHistory()
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) { if (_binding != null) loadHistory() }
        })
    }

    private fun loadHistory() {
        val token = prefManager.getToken() ?: ""
        val userId = prefManager.getUserId()
        
        // Menggunakan getAllTransactions karena endpoint per user tidak tersedia
        ApiConfig.getApiService().getAllTransactions("Bearer $token").enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    binding.progressBarHistory.visibility = View.GONE
                    if (response.isSuccessful) {
                        val rawData = response.body()?.data ?: emptyList()
                        // Filter manual berdasarkan user yang login
                        allHistory = rawData.filter { it.userId == userId }
                        
                        checkStatusAndSyncStock(allHistory)
                        historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines, allPublishers, allGenres)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarHistory.visibility = View.GONE
            }
        })
    }

    private fun checkStatusAndSyncStock(historyList: List<HistoryDataItem>) {
        for (item in historyList) {
            val status = item.status.lowercase()
            
            if (status == "dipinjam") {
                if (!prefManager.isStatusNotified(item.id, "stok_kurang")) {
                    Log.d("StockSync", "Peminjaman Terdeteksi (ID: ${item.id}). Mengurangi stok...")
                    updateBookStock(item.bukuId, -1, item.id, "stok_kurang")
                }
            } else if (status == "dikembalikan") {
                // Pastikan stok dikurangi dulu jika status loncat
                if (!prefManager.isStatusNotified(item.id, "stok_kurang")) {
                    updateBookStock(item.bukuId, -1, item.id, "stok_kurang")
                }
                // Tambah stok karena buku kembali
                if (!prefManager.isStatusNotified(item.id, "stok_tambah")) {
                    Log.d("StockSync", "Pengembalian Terdeteksi (ID: ${item.id}). Menambah stok...")
                    updateBookStock(item.bukuId, 1, item.id, "stok_tambah")
                    
                    if (!prefManager.isSuccessScreenShown(item.id)) {
                        prefManager.setSuccessScreenShown(item.id)
                        val book = allBooks.find { it.id == item.bukuId }
                        val bundle = Bundle().apply { putString("book_title", book?.judulBuku ?: "Buku") }
                        findNavController().navigate(R.id.successReturnFragment, bundle)
                    }
                }
            }
        }
    }

    private fun updateBookStock(bookId: Int, change: Int, transactionId: Int, flagKey: String) {
        val loginRequest = LoginRequest("admin", "admin123")
        ApiConfig.getApiService().login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                val adminToken = response.body()?.data?.token
                if (adminToken != null) {
                    performStockUpdate(adminToken, bookId, change, transactionId, flagKey)
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("StockSync", "Admin login failed: ${t.message}")
            }
        })
    }

    private fun performStockUpdate(adminToken: String, bookId: Int, change: Int, transactionId: Int, flagKey: String) {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                val book = response.body()?.data?.find { it.id == bookId }
                if (book != null) {
                    val newStock = (book.stok + change).coerceAtLeast(0)


                    val updateMap = mutableMapOf<String, Any>(
                        "judul_buku" to book.judulBuku,
                        "deskripsi" to (book.deskripsi ?: ""),
                        "stok" to newStock.toString(),
                        "tgl_terbit" to (book.tglTerbit ?: ""),
                        "genre_id" to book.genreId.toString(),
                        "penulis_id" to book.penulisId.toString(),
                        "penerbit_id" to book.penerbitId.toString()
                    )

                    ApiConfig.getApiService().updateBook("Bearer $adminToken", bookId, updateMap)
                        .enqueue(object : Callback<UpdateBookResponse> {
                            override fun onResponse(call: Call<UpdateBookResponse>, response: Response<UpdateBookResponse>) {
                                if (response.isSuccessful) {
                                    Log.d("StockSync", "BERHASIL Update Stok Buku $bookId jadi $newStock")
                                    prefManager.setStatusNotified(transactionId, flagKey)
                                } else {
                                    Log.e("StockSync", "GAGAL Update: ${response.errorBody()?.string()}")
                                }
                            }
                            override fun onFailure(call: Call<UpdateBookResponse>, t: Throwable) {
                                Log.e("StockSync", "Network Error: ${t.message}")
                            }
                        })
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                Log.e("StockSync", "Gagal mengambil data buku: ${t.message}")
            }
        })
    }

    private fun applyChipStyles() {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val bgColors = intArrayOf("#DBEAFE".toColorInt(), "#F1F5F9".toColorInt())
        val textColors = intArrayOf("#1E40AF".toColorInt(), "#64748B".toColorInt())
        val colorStateListBg = ColorStateList(states, bgColors)
        val colorStateListText = ColorStateList(states, textColors)
        val chips = listOf(binding.chipAll, binding.chipPending, binding.chipDipinjam, binding.chipDikembalikan)
        for (chip in chips) {
            chip.chipBackgroundColor = colorStateListBg
            chip.setTextColor(colorStateListText)
            chip.chipStrokeWidth = 0f
        }
    }
    private fun loadRequiredData() {
        binding.progressBarHistory.visibility = View.VISIBLE
        // Muat Penulis
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    loadPublishers() // Lanjut muat Penerbit
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) { loadPublishers() }
        })
    }

    private fun loadPublishers() {
        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (response.isSuccessful) allPublishers = response.body()?.data ?: emptyList()
                loadGenres() // Lanjut muat Genre
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) { loadGenres() }
        })
    }

    private fun loadGenres() {
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (response.isSuccessful) allGenres = response.body()?.data ?: emptyList()
                loadBooks() // Terakhir muat Buku
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) { loadBooks() }
        })
    }


    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(allHistory, allBooks, allAuthors, allFines, allPublishers, allGenres) { item ->
            val book = allBooks.find { it.id == item.bukuId }
            if (book != null) {
                val author = allAuthors.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
                val publisher = allPublishers.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
                val genre = allGenres.find { it.id == book.genreId }?.namaGenre ?: "Umum"

                val bundle = Bundle().apply {
                    putParcelable("history", item)
                    putParcelable("book", book)
                    putString("book_writer", author)
                    putString("book_publisher", publisher)
                    putString("book_genre", genre)
                }
                findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment, bundle)
            }
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtered = when (checkedIds.firstOrNull()) {
                R.id.chip_pending -> allHistory.filter { it.status.lowercase() == "pending" }
                R.id.chip_dipinjam -> allHistory.filter { it.status.lowercase() == "dipinjam" }
                R.id.chip_dikembalikan -> allHistory.filter { it.status.lowercase() == "dikembalikan" }
                else -> allHistory
            }
            historyAdapter.updateData(filtered, allBooks, allAuthors, allFines, allPublishers, allGenres)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
