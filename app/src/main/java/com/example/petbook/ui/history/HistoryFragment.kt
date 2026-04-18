package com.example.petbook.ui.history

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
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.AuthorResponse
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.BookResponse
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.FineResponse
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.api.model.HistoryResponse
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
        historyAdapter = HistoryAdapter(emptyList(), emptyList(), emptyList(), emptyList()) { item ->
            findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun loadRequiredData() {
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
                    loadHistory()
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                if (_binding != null) loadHistory()
            }
        })
    }

    private fun loadHistory() {
        val token = prefManager.getToken()
        val currentUserId = prefManager.getUserId()
        
        if (token.isNullOrEmpty()) return

        val authHeader = "Bearer $token"
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val rawHistory = response.body()?.data ?: emptyList()
                    allHistory = rawHistory.filter { it.userId == currentUserId }
                    historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines)
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                Log.e("History", "Error: ${t.message}")
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