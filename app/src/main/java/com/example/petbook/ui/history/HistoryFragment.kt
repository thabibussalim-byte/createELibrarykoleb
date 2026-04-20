package com.example.petbook.ui.history

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
import com.example.petbook.databinding.FragmentHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        loadRequiredData()
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
                    if (response.isSuccessful) {
                        binding.progressBarHistory.visibility = View.GONE
                        allHistory = response.body()?.data ?: emptyList()
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
                        historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarHistory.visibility = View.GONE
            }
        })
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