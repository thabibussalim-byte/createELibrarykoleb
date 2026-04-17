package com.example.petbook.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.petbook.data.api.model.GenreItem
import com.example.petbook.data.api.model.GenreResponse
import com.example.petbook.data.api.model.PublisherItem
import com.example.petbook.data.api.model.PublisherResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var rekomendasiAdapter: BookAdapter
    private lateinit var populerAdapter: BookAdapter
    
    private var allBooksList: List<BookItem> = emptyList()
    private var allAuthorsList: List<AuthorItem> = emptyList()
    private var allPublishersList: List<PublisherItem> = emptyList()
    private var allGenresList: List<GenreItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupRecyclerView()
        setupSearch()
        loadGenres()
    }

    private fun setupGreeting() {
        val prefManager = PreferenceManager(requireContext())
        val username = prefManager.getUsername()
        if (!username.isNullOrEmpty()) {
            binding.tvWelcome.text = "Hai $username, mau baca buku apa hari ini?"
        }
    }

    private fun loadGenres() {
        binding.progressBar.visibility = View.VISIBLE
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allGenresList = response.body()?.data ?: emptyList()
                    
                    binding.chipGroupCategory.removeAllViews()
                    for (genre in allGenresList.take(6)) {
                        addChipToGroup(genre.id, genre.namaGenre)
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
                    allAuthorsList = response.body()?.data ?: emptyList()
                    rekomendasiAdapter.updateAuthors(allAuthorsList)
                    populerAdapter.updateAuthors(allAuthorsList)
                    loadPublishers()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                if (_binding != null) loadPublishers()
            }
        })
    }

    private fun loadPublishers() {
        ApiConfig.getApiService().getPublishers().enqueue(object : Callback<PublisherResponse> {
            override fun onResponse(call: Call<PublisherResponse>, response: Response<PublisherResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allPublishersList = response.body()?.data ?: emptyList()
                    getBooksFromApi()
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) {
                if (_binding != null) getBooksFromApi()
            }
        })
    }

    private fun getBooksFromApi() {
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        allBooksList = response.body()?.data ?: emptyList()
                        updateDisplay(allBooksList)
                    }
                }
            }
            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filterBooks(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterBooks(query: String) {
        val filteredList = if (query.isEmpty()) {
            allBooksList
        } else {
            allBooksList.filter { book ->
                val authorName = allAuthorsList.find { it.id == book.penulisId }?.namaPenulis?.lowercase() ?: ""
                book.judulBuku.lowercase().contains(query) || authorName.contains(query)
            }
        }
        updateDisplay(filteredList)
    }

    private fun setupRecyclerView() {
        rekomendasiAdapter = BookAdapter(emptyList(), emptyList()) { book, rating ->
            navigateToDetail(book, rating)
        }
        populerAdapter = BookAdapter(emptyList(), emptyList()) { book, rating ->
            navigateToDetail(book, rating)
        }

        binding.rvRekomendasi.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = rekomendasiAdapter
        }

        binding.rvPopuler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = populerAdapter
        }
    }

    private fun navigateToDetail(book: BookItem, rating: Float) {
        val authorName = allAuthorsList.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
        val publisherName = allPublishersList.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
        val genreName = allGenresList.find { it.id == book.genreId }?.namaGenre ?: "Umum"
        
        val bundle = Bundle().apply {
            putParcelable("book", book)
            putString("book_writer", authorName)
            putString("book_publisher", publisherName)
            putString("book_genre", genreName)
            putFloat("book_rating", rating) // Kirim rating
        }
        findNavController().navigate(R.id.action_homeFragment_to_detailbookFragment, bundle)
    }

    private fun updateDisplay(listBuku: List<BookItem>) {
        rekomendasiAdapter.submitList(listBuku.take(5))
        populerAdapter.submitList(listBuku.drop(5))
    }

    private fun addChipToGroup(genreId: Int, name: String) {
        if (_binding == null) return
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) updateDisplay(allBooksList.filter { it.genreId == genreId })
            else if (binding.chipGroupCategory.checkedChipId == View.NO_ID) updateDisplay(allBooksList)
        }
        binding.chipGroupCategory.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}