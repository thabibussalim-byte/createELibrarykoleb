package com.example.petbook.ui.history

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.local.datastore.SettingPreferences
import com.example.petbook.data.local.datastore.ViewModelFactory
import com.example.petbook.data.local.datastore.dataStore
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentHistoryBinding
import com.example.petbook.utils.DataMapper.toBookItem
import com.example.petbook.utils.DataMapper.toDataItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var prefManager: PreferenceManager
    
    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory.getInstance(
            requireContext(),
            SettingPreferences.getInstance(requireContext().dataStore)
        )
    }

    private var allHistory: List<HistoryDataItem> = emptyList()
    private var allBooks: List<BookItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()
    private var allFines: List<FineDataItem> = emptyList()
    private var allPublishers: List<PublisherItem> = emptyList()
    private var allGenres: List<GenreItem> = emptyList()

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
        
        val userId = prefManager.getUserId()

        viewModel.getAllBooks().asLiveData().observe(viewLifecycleOwner) { bookEntities ->
            allBooks = bookEntities.map { it.toBookItem() }
            refreshHistoryAdapter()
        }

        viewModel.getHistoryByUserId(userId).asLiveData().observe(viewLifecycleOwner) { histories ->
            if (histories.isNotEmpty()) {
                binding.progressBarHistory.visibility = View.GONE
                allHistory = histories.map { it.toDataItem() }
                refreshHistoryAdapter()
            } else {
                binding.progressBarHistory.visibility = View.VISIBLE
            }
        }

        loadSupportingData()

        viewModel.refreshHistory(userId)
        viewModel.refreshBooks() 
    }

    private fun refreshHistoryAdapter() {
        if (_binding != null) {
            historyAdapter.updateData(allHistory, allBooks, allAuthors, allFines)
        }
    }

    private fun loadSupportingData() {
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    refreshHistoryAdapter()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {}
        })

        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allGenres = response.body()?.data ?: emptyList()
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {}
        })

        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allPublishers = response.body()?.data ?: emptyList()
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) {}
        })

        val token = prefManager.getToken()
        if (!token.isNullOrEmpty()) {
            ApiConfig.getApiService().getFines("Bearer $token").enqueue(object : Callback<FineResponse> {
                override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                    if (_binding != null && response.isSuccessful) {
                        allFines = response.body()?.data ?: emptyList()
                        refreshHistoryAdapter()
                    }
                }
                override fun onFailure(call: Call<FineResponse>, t: Throwable) {}
            })
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(allHistory, allBooks, allAuthors, allFines) { historyItem ->
            val book = allBooks.find { it.id == historyItem.bukuId }
            if (book != null) {
                val author = allAuthors.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
                val publisher = allPublishers.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
                val genre = allGenres.find { it.id == book.genreId }?.namaGenre ?: "Umum"
                val fine = allFines.find { it.transaksiId == historyItem.id }

                val bundle = Bundle().apply {
                    putParcelable("history", historyItem)
                    putParcelable("book", book)
                    putString("book_writer", author)
                    putString("book_publisher", publisher)
                    putString("book_genre", genre)
                    putParcelable("fine", fine)
                    putBoolean("is_history", true)
                }
                findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment, bundle)
            }
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
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

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtered = when (checkedIds.firstOrNull()) {
                R.id.chip_pending -> allHistory.filter { it.status.lowercase() == "pending" }
                R.id.chip_dipinjam -> allHistory.filter { it.status.lowercase() == "dipinjam" }
                R.id.chip_dikembalikan -> allHistory.filter { it.status.lowercase() == "dikembalikan" }
                else -> allHistory
            }
            historyAdapter.updateData(filtered, allBooks, allAuthors, allFines)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
