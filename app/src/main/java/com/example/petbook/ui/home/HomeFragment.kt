package com.example.petbook.ui.home

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var rekomendasiAdapter: BookAdapter
    private lateinit var populerAdapter: BookAdapter
    private lateinit var prefManager: PreferenceManager
    
    private var allBooksList: List<BookItem> = emptyList()
    private var authorList: List<AuthorItem> = emptyList()
    private var genreList: List<GenreItem> = emptyList()
    private var publisherList: List<PublisherItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupSearch()
        setupSeeAllButtons()
        loadGenres()
        checkGlobalHistoryStatus()
    }

    override fun onResume() {
        super.onResume()
        setupUI() 
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val username = prefManager.getUsername()
        if (!username.isNullOrEmpty()) {
            binding.tvWelcome.text = "Hai $username, mau baca apa?"
        }
    }

    private fun checkGlobalHistoryStatus() {
        val userId = prefManager.getUserId()
        val token = prefManager.getToken() ?: ""
        if (token.isEmpty()) return

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val rawData = response.body()?.data ?: emptyList()
                    val histories = rawData.filter { it.userId == userId }

                    for (item in histories) {
                        val status = item.status.lowercase()

                        if ((status == "dipinjam" || status == "dikembalikan" || status == "selesai") &&
                            !prefManager.isStatusSeen(item.id, status) &&
                            isWithinTwentyMinutes(item.updatedAt)) {

                            prefManager.setStatusSeen(item.id, status)

                            val bundle = Bundle().apply {
                                putString("book_title", "Buku Anda")
                                putString("status", status)
                            }
                            findNavController().navigate(R.id.successReturnFragment, bundle)
                            break
                        }
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {}
        })
    }

    private fun isWithinTwentyMinutes(updatedAt: String): Boolean {
        return try {

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val updateDate = sdf.parse(updatedAt)
            val currentTime = System.currentTimeMillis()
            val diffInMillis = currentTime - (updateDate?.time ?: 0)

            diffInMillis <= 20 * 60 * 1000 && diffInMillis >= 0
        } catch (e: Exception) {
            false
        }
    }

    private fun setupSeeAllButtons() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, true) 
            .setLaunchSingleTop(true)
            .build()

        binding.tvRekomendasiAll.setOnClickListener {
            findNavController().navigate(R.id.bookFragment, null, navOptions)
        }
        binding.tvPopulerAll.setOnClickListener {
            findNavController().navigate(R.id.bookFragment, null, navOptions)
        }
    }

    private fun loadGenres() {
        binding.progressBar.visibility = View.VISIBLE
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    genreList = response.body()?.data ?: emptyList()
                    binding.chipGroupCategory.removeAllViews()
                    for (genre in genreList.take(6)) {
                        addChipToGroup(genre.id, genre.namaGenre, false)
                    }
                    loadAuthors()
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) { if (_binding != null) loadAuthors() }
        })
    }

    private fun loadAuthors() {
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    authorList = response.body()?.data ?: emptyList()
                    rekomendasiAdapter.updateAuthors(authorList)
                    populerAdapter.updateAuthors(authorList)
                    loadPublishers()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) { if (_binding != null) loadPublishers() }
        })
    }

    private fun loadPublishers() {
        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (_binding != null && response.isSuccessful) {
                    publisherList = response.body()?.data ?: emptyList()
                    getBooksFromApi()
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) { if (_binding != null) getBooksFromApi() }
        })
    }

    private fun getBooksFromApi() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        allBooksList = response.body()?.data ?: emptyList()
                        updateDisplay(allBooksList)
                    }
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) { if (_binding != null) binding.progressBar.visibility = View.GONE }
        })
    }

    private fun setupSearch() {
        binding.searchViewHome.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText ?: "")
                return true
            }
        })
    }

    private fun filterBooks(query: String) {
        val filteredList = if (query.isEmpty()) allBooksList
        else {
            allBooksList.filter { book ->
                val authorName = authorList.find { it.id == book.penulisId }?.namaPenulis?.lowercase() ?: ""
                book.judulBuku.lowercase().contains(query.lowercase()) || authorName.contains(query.lowercase())
            }
        }
        
        if (filteredList.isEmpty() && query.isNotEmpty()) {
            binding.layoutHomeContent.visibility = View.GONE
            binding.layoutEmptySearch.visibility = View.VISIBLE
        } else {
            binding.layoutHomeContent.visibility = View.VISIBLE
            binding.layoutEmptySearch.visibility = View.GONE
            updateDisplay(filteredList)
        }
    }

    private fun setupRecyclerView() {
        rekomendasiAdapter = BookAdapter(emptyList(), authorList) { book -> navigateToDetail(book) }
        populerAdapter = BookAdapter(emptyList(), authorList) { book -> navigateToDetail(book) }

        binding.rvRekomendasi.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = rekomendasiAdapter
        }
        binding.rvPopuler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = populerAdapter
        }
    }

    private fun navigateToDetail(book: BookItem) {
        val authorName = authorList.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
        val genreName = genreList.find { it.id == book.genreId }?.namaGenre ?: "Umum"
        val publisherName = publisherList.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
        
        val bundle = Bundle().apply {
            putParcelable("book", book)
            putString("book_writer", authorName)
            putString("book_genre", genreName)
            putString("book_publisher", publisherName)
        }
        findNavController().navigate(R.id.action_homeFragment_to_detailbookFragment, bundle)
    }

    private fun updateDisplay(listBuku: List<BookItem>) {
        rekomendasiAdapter.submitList(listBuku.take(5))
        populerAdapter.submitList(listBuku.drop(5))
    }

    private fun addChipToGroup(genreId: Int, name: String, isDefault: Boolean) {
        if (_binding == null) return
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isDefault
        
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val colors = intArrayOf("#DBEAFE".toColorInt(), "#F1F5F9".toColorInt())
        chip.chipBackgroundColor = ColorStateList(states, colors)
        chip.setTextColor(ColorStateList(states, intArrayOf("#1E40AF".toColorInt(), "#64748B".toColorInt())))
        chip.chipStrokeWidth = 0f

        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val filtered = allBooksList.filter { it.genreId == genreId }
                if (filtered.isEmpty()) {
                    binding.layoutHomeContent.visibility = View.GONE
                    binding.layoutEmptySearch.visibility = View.VISIBLE
                } else {
                    binding.layoutHomeContent.visibility = View.VISIBLE
                    binding.layoutEmptySearch.visibility = View.GONE
                    updateDisplay(filtered)
                }
                binding.searchViewHome.setQuery("", false)
            } else if (binding.chipGroupCategory.checkedChipId == View.NO_ID) {
                binding.layoutHomeContent.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
                updateDisplay(allBooksList)
            }
        }
        binding.chipGroupCategory.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
