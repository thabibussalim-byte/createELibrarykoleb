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
        applyChipStyles()
        setupFilters()
        loadRequiredData()
    }

    private fun applyChipStyles() {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val backgroundColors = intArrayOf(Color.parseColor("#DBEAFE"), Color.parseColor("#F1F5F9"))
        val textColors = intArrayOf(Color.parseColor("#1E40AF"), Color.parseColor("#64748B"))

        val colorStateListBg = ColorStateList(states, backgroundColors)
        val colorStateListText = ColorStateList(states, textColors)

        val chips = listOf(binding.chipAll, binding.chipPending, binding.chipDipinjam, binding.chipDikembalikan)
        for (chip in chips) {
            chip.chipBackgroundColor = colorStateListBg
            chip.setTextColor(colorStateListText)
            chip.chipStrokeWidth = 0f
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList(), emptyList(), emptyList(), emptyList()) { historyItem ->
            val book = allBooks.find { it.id == historyItem.bukuId }
            val author = allAuthors.find { it.id == book?.penulisId }?.namaPenulis ?: "Penulis Anonim"
            
            // PERBAIKAN: Ambil denda TERAKHIR (terbaru) untuk transaksi ini
            val fine = allFines.findLast { it.transaksiId == historyItem.id }

            if (book != null) {
                val bundle = Bundle().apply {
                    putParcelable("history", historyItem)
                    putParcelable("book", book)
                    putString("author", author)
                    putParcelable("fine", fine)
                }
                findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment, bundle)
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
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) { if (_binding != null) loadBooks() }
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
            override fun onFailure(call: Call<BookResponse>, t: Throwable) { if (_binding != null) loadFines() }
        })
    }

    private fun loadFines() {
        val token = prefManager.getToken()
        if (token.isNullOrEmpty()) { loadHistory(); return }

        ApiConfig.getApiService().getFines("Bearer $token").enqueue(object : Callback<FineResponse> {
            override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allFines = response.body()?.data ?: emptyList()
                }
                loadHistory()
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) { if (_binding != null) loadHistory() }
        })
    }

    private fun loadHistory() {
        val token = prefManager.getToken()
        val userId = prefManager.getUserId()
        if (token.isNullOrEmpty()) {
            binding.progressBarHistory.visibility = View.GONE
            return
        }

        ApiConfig.getApiService().getHistoryByUser("Bearer $token", userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    binding.progressBarHistory.visibility = View.GONE
                    if (response.isSuccessful) {
                        allHistory = response.body()?.data ?: emptyList()
                        updateUI(allHistory)
                    } else {
                        loadAllTransactionsFallback("Bearer $token", userId)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) { loadAllTransactionsFallback("Bearer $token", userId) }
        })
    }

    private fun loadAllTransactionsFallback(token: String, userId: Int) {
        ApiConfig.getApiService().getAllTransactions(token).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    binding.progressBarHistory.visibility = View.GONE
                    if (response.isSuccessful) {
                        allHistory = (response.body()?.data ?: emptyList()).filter { it.userId == userId }
                        updateUI(allHistory)
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
            updateUI(filteredList)
        }
    }

    private fun updateUI(list: List<HistoryDataItem>) {
        if (list.isEmpty()) {
            binding.rvHistory.visibility = View.GONE
            binding.layoutEmptyHistory.visibility = View.VISIBLE
        } else {
            binding.rvHistory.visibility = View.VISIBLE
            binding.layoutEmptyHistory.visibility = View.GONE
            
            // PERBAIKAN: Ambil denda terbaru per transaksi
            val latestFines = allFines.reversed().distinctBy { it.transaksiId }
            historyAdapter.updateData(list, allBooks, allAuthors, latestFines)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
