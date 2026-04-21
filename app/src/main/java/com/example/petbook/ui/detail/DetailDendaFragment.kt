package com.example.petbook.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.AuthorResponse
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.BookResponse
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.FineResponse
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.api.model.HistoryResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailDendaBinding
import com.example.petbook.ui.history.HistoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailDendaFragment : Fragment() {

    private var _binding: FragmentDetailDendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    
    private var userTransactions: List<HistoryDataItem> = emptyList()
    private var allBooks: List<BookItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()
    private var userFines: List<FineDataItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailDendaBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadInitialData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList(), emptyList(), emptyList(), emptyList()) {
            // Logika klik jika diperlukan
        }

        binding.rvBukuDenda.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadInitialData() {
        binding.progressBarDenda.visibility = View.VISIBLE
        
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    loadBooks()
                } else {
                    handleError("Gagal memuat data penulis")
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                handleError(t.message)
            }
        })
    }

    private fun loadBooks() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allBooks = response.body()?.data ?: emptyList()
                    loadTransactions()
                } else {
                    handleError("Gagal memuat data buku")
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                handleError(t.message)
            }
        })
    }

    private fun loadTransactions() {
        val userId = prefManager.getUserId()
        val token = prefManager.getToken()
        
        if (token.isNullOrEmpty()) {
            handleError("Sesi habis, silakan login kembali")
            return
        }

        val authHeader = "Bearer $token"
        
        // Menggunakan getAllTransactions dan filter manual untuk menghindari 404 pada endpoint per-user
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val allTransactions = response.body()?.data ?: emptyList()
                    // Filter transaksi milik user yang sedang login
                    userTransactions = allTransactions.filter { it.userId == userId }
                    loadFines(authHeader)
                } else {
                    handleError("Gagal memuat riwayat transaksi")
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                handleError(t.message)
            }
        })
    }

    private fun loadFines(authHeader: String) {
        ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
            override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                if (_binding != null) {
                    binding.progressBarDenda.visibility = View.GONE
                    if (response.isSuccessful) {
                        val allFines = response.body()?.data ?: emptyList()

                        // Ambil daftar ID transaksi milik user ini
                        val userTransactionIds = userTransactions.map { it.id }

                        // Filter denda: Cari yang transaksi_id nya ada di riwayat transaksi user ini
                        userFines = allFines.filter { fine ->
                            userTransactionIds.contains(fine.transaksiId)
                        }

                        updateUI()
                    }
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                handleError(t.message)
            }
        })
    }

    private fun updateUI() {

       // Menjumlahkan HANYA denda yang memiliki status "belumdibayar"
       val totalDendaBelumBayar = userFines
           .filter { it.status == "belumdibayar" }
           .sumOf { it.totalDenda.toIntOrNull() ?: 0 }

        // Tampilkan total denda yang belum dibayar di Header
            binding.tvDetailTotalDenda.text = "Rp $totalDendaBelumBayar"
        
        // Jumlah buku bermasalah (semua buku yang ada di list denda user)
        binding.tvJumlahBukuDenda.text = "${userFines.size} Tidak di temukan"

        // Ambil ID transaksi yang memiliki denda (baik lunas maupun belum)
        val fineTransactionIds = userFines.map { it.transaksiId }
        val transactionsWithFine = userTransactions.filter { it.id in fineTransactionIds }

        // Urutkan transaksi terbaru di atas
        val sortedTransactions = transactionsWithFine.sortedByDescending { it.id }

        // Update adapter dengan data denda yang sudah difilter
        historyAdapter.updateData(sortedTransactions, allBooks, allAuthors, userFines)

        if (userFines.isEmpty()) {
            binding.rvBukuDenda.visibility = View.GONE
        } else {
            binding.rvBukuDenda.visibility = View.VISIBLE
        }
    }

    private fun handleError(message: String?) {
        if (_binding != null) {
            binding.progressBarDenda.visibility = View.GONE
            Toast.makeText(requireContext(), message ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
