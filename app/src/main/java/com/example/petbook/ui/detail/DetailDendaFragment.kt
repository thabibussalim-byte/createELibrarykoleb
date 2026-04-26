package com.example.petbook.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailDendaBinding
import com.example.petbook.ui.history.HistoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DetailDendaFragment : Fragment() {

    private var _binding: FragmentDetailDendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    
    private var userTransactions: List<HistoryDataItem> = emptyList()
    private var allBooks: List<BookItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()
    private var allGenres: List<GenreItem> = emptyList()
    private var allPublishers: List<PublisherItem> = emptyList()
    private var unpaidFines: List<FineDataItem> = emptyList()

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
        historyAdapter = HistoryAdapter(emptyList(), emptyList(), emptyList(), emptyList()) { history ->
            val book = allBooks.find { it.id == history.bukuId }
            val author = allAuthors.find { it.id == book?.penulisId }?.namaPenulis ?: "Penulis Anonim"
            val genre = allGenres.find { it.id == book?.genreId }?.namaGenre ?: "Umum"
            val publisher = allPublishers.find { it.id == book?.penerbitId }?.publisherName ?: "Penerbit Anonim"
            
            val fine = unpaidFines.find { it.transaksiId == history.id }

            val bundle = Bundle().apply {
                putParcelable("history", history)
                putParcelable("book", book)
                putParcelable("fine", fine)
                putString("book_writer", author)
                putString("book_genre", genre)
                putString("book_publisher", publisher)
            }
            
            findNavController().navigate(R.id.action_detailDendaFragment_to_detailHistoryFragment, bundle)
        }
        
        binding.rvBukuDenda.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadInitialData() {
        binding.progressBarDenda.visibility = View.VISIBLE
        loadSupportingData()
    }

    private fun loadSupportingData() {
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    checkAndLoadMainData()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {}
        })

        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allGenres = response.body()?.data ?: emptyList()
                    checkAndLoadMainData()
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {}
        })

        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allPublishers = response.body()?.data ?: emptyList()
                    checkAndLoadMainData()
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) {}
        })
    }

    private var supportingDataCount = 0
    private fun checkAndLoadMainData() {
        supportingDataCount++
        if (supportingDataCount >= 3) {
            loadBooks()
        }
    }

    private fun loadBooks() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allBooks = response.body()?.data ?: emptyList()
                    loadTransactions()
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                handleError("Gagal Buku: ${t.message}")
            }
        })
    }

    private fun loadTransactions() {
        val userId = prefManager.getUserId()
        val token = prefManager.getToken()
        if (token.isNullOrEmpty()) return

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    if (response.isSuccessful) {
                        val rawData = response.body()?.data ?: emptyList()
                        userTransactions = rawData.filter { it.userId == userId }
                        loadFines(authHeader)
                    } else {
                        handleError("Gagal memuat riwayat: ${response.message()}")
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                handleError("Gagal koneksi: ${t.message}")
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
                        val transactionIds = userTransactions.map { it.id }
                        
                        unpaidFines = allFines.filter { 
                            it.transaksiId in transactionIds && it.status.lowercase() != "dibayar" 
                        }
                        
                        updateUI()
                    }
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                handleError("Gagal Denda: ${t.message}")
            }
        })
    }

    private fun updateUI() {
        var total = 0
        unpaidFines.forEach { fine ->
            val cleanAmount = fine.totalDenda.replace(Regex("[^0-9]"), "")
            total += cleanAmount.toIntOrNull() ?: 0
        }
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvDetailTotalDenda.text = formatRupiah.format(total).replace(",00", "").replace("Rp", "Rp: ")
        binding.tvJumlahBukuDenda.text = "${unpaidFines.size} Buku"

        val fineTransactionIds = unpaidFines.map { it.transaksiId }
        val transactionsWithFine = userTransactions.filter { it.id in fineTransactionIds }

        historyAdapter.updateData(transactionsWithFine, allBooks, allAuthors, unpaidFines)
        
        binding.tvInstruction.visibility = if (total > 0) View.VISIBLE else View.GONE
    }

    private fun handleError(msg: String) {
        if (_binding != null) {
            binding.progressBarDenda.visibility = View.GONE
            Log.e("DetailDenda", msg)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
