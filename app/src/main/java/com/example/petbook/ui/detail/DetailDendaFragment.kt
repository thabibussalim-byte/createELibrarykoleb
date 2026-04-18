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
        
        ApiConfig.getApiService().getHistoryByUser(authHeader, userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    userTransactions = response.body()?.data ?: emptyList()
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
                        
                        // Filter denda yang HANYA milik transaksi user ini
                        val transactionIds = userTransactions.map { it.id }
                        userFines = allFines.filter { it.transaksiId in transactionIds }
                        
                        updateUI()
                    } else {
                        handleError("Gagal memuat data denda")
                    }
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                handleError(t.message)
            }
        })
    }

    private fun updateUI() {
        var total = 0
        userFines.forEach { 
            total += it.totalDenda.toIntOrNull() ?: 0 
        }

        binding.tvDetailTotalDenda.text = "Rp: $total"
        binding.tvJumlahBukuDenda.text = "${userFines.size} Buku Bermasalah"

        val fineTransactionIds = userFines.map { it.transaksiId }
        val transactionsWithFine = userTransactions.filter { it.id in fineTransactionIds }

        historyAdapter.updateData(transactionsWithFine, allBooks, allAuthors, userFines)
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