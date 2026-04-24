package com.example.petbook.ui.buku

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
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
import com.example.petbook.databinding.FragmentBookBinding
import com.example.petbook.ui.history.HistoryViewModel
import com.example.petbook.utils.DataMapper.toBookItem
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookKatalogAdapter: BookKatalogAdapter

    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext(), SettingPreferences.getInstance(requireContext().dataStore))
    }

    private var allBooks: List<BookItem> = emptyList()
    private var allPublishers: List<PublisherItem> = emptyList()
    private var allAuthors: List<AuthorItem> = emptyList()
    private var allGenres: List<GenreItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()

        viewModel.getAllBooks().asLiveData().observe(viewLifecycleOwner) { entities ->
            if (entities.isNotEmpty()) {
                binding.progressBarBook.visibility = View.GONE
                val bookItems = entities.map { it.toBookItem() }
                allBooks = bookItems
                bookKatalogAdapter.updateData(bookItems)
                binding.tvStatusLabel.text = "Menampilkan semua koleksi"
                binding.layoutEmptyState.visibility = View.GONE
            } else {
                // Hanya muncul jika data di HP benar-benar kosong
                binding.progressBarBook.visibility = View.VISIBLE
            }
        }

        // 2. Refresh data dari API di background
        viewModel.refreshBooks()

        // 3. Load data pendukung (Genre, Author, Publisher)
        loadGenres()
    }

    private fun loadGenres() {
        // Jangan paksa ProgressBar muncul jika sudah ada buku yang tampil
        if (allBooks.isEmpty()) binding.progressBarBook.visibility = View.VISIBLE

        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allGenres = response.body()?.data ?: emptyList()
                    binding.chipGroupBookFilter.removeAllViews()
                    addChipToGroup(-1, "Semua", true)
                    for (genre in allGenres) {
                        addChipToGroup(genre.id, genre.namaGenre, false)
                    }
                    loadAuthors()
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                if (_binding != null) loadAuthors()
            }
        })
    }

    private fun loadAuthors() {
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthors = response.body()?.data ?: emptyList()
                    bookKatalogAdapter.updateAuthors(allAuthors)
                    fetchPublishers()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                if (_binding != null) fetchPublishers()
            }
        })
    }

    private fun fetchPublishers() {
        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (_binding != null) {
                    // Sembunyikan progress bar setelah semua data pendukung selesai
                    binding.progressBarBook.visibility = View.GONE
                    if (response.isSuccessful) {
                        allPublishers = response.body()?.data ?: emptyList()
                    }
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarBook.visibility = View.GONE
            }
        })
    }

    // Fungsi getBooksFromApi() dihapus karena sudah digantikan oleh viewModel.refreshBooks()

    private fun setupRecyclerView() {
        bookKatalogAdapter = BookKatalogAdapter(emptyList(), emptyList()) { book, rating ->
            val authorName = allAuthors.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
            val publisherName = allPublishers.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
            val genreName = allGenres.find { it.id == book.genreId }?.namaGenre ?: "Umum"

            val bundle = Bundle().apply {
                putParcelable("book", book)
                putString("book_writer", authorName)
                putString("book_publisher", publisherName)
                putString("book_genre", genreName)
                putFloat("book_rating", rating)
            }
            findNavController().navigate(R.id.action_bookFragment_to_detailbookFragment, bundle)
        }

        binding.rvAllBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookKatalogAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchViewBook.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText ?: "")
                return true
            }
        })
    }

    private fun filterBooks(query: String) {
        if (query.isEmpty()) {
            bookKatalogAdapter.updateData(allBooks)
            binding.tvStatusLabel.text = "Menampilkan semua koleksi"
            binding.layoutEmptyState.visibility = View.GONE
            return
        }

        val filteredList = allBooks.filter { book ->
            val authorName = allAuthors.find { it.id == book.penulisId }?.namaPenulis ?: ""
            book.judulBuku.contains(query, ignoreCase = true) ||
                    authorName.contains(query, ignoreCase = true)
        }

        bookKatalogAdapter.updateData(filteredList)
        binding.tvStatusLabel.text = "Hasil pencarian: '${query}' (${filteredList.size} buku)"
        binding.layoutEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun addChipToGroup(genreId: Int, name: String, isDefault: Boolean) {
        if (_binding == null) return
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isDefault

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val backgroundColors = intArrayOf(Color.parseColor("#DBEAFE"), Color.parseColor("#F1F5F9"))
        chip.chipBackgroundColor = ColorStateList(states, backgroundColors)

        val textColors = intArrayOf(Color.parseColor("#1E40AF"), Color.parseColor("#64748B"))
        chip.setTextColor(ColorStateList(states, textColors))
        chip.chipStrokeWidth = 0f

        chip.setOnClickListener {
            if (genreId == -1) {
                bookKatalogAdapter.updateData(allBooks)
                binding.tvStatusLabel.text = "Menampilkan semua koleksi"
            } else {
                val filtered = allBooks.filter { it.genreId == genreId }
                bookKatalogAdapter.updateData(filtered)
                binding.tvStatusLabel.text = "Kategori: $name (${filtered.size} buku)"
            }
            binding.searchViewBook.setQuery("", false)
        }
        binding.chipGroupBookFilter.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}