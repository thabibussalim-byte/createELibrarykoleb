package com.example.elibrarypetik.ui.home

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
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.api.ApiConfig
import com.example.elibrarypetik.data.api.model.AuthorItem
import com.example.elibrarypetik.data.api.model.AuthorResponse
import com.example.elibrarypetik.data.api.model.BookItem
import com.example.elibrarypetik.data.api.model.BookResponse
import com.example.elibrarypetik.data.api.model.GenreResponse
import com.example.elibrarypetik.data.pref.PreferenceManager
import com.example.elibrarypetik.databinding.FragmentHomeBinding
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
    private var allAuthorsList: List<AuthorItem> = emptyList() // Simpan daftar penulis

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
        getGenreFromApi()
        loadDataFromServer()
    }

    private fun setupGreeting() {
        val prefManager = PreferenceManager(requireContext())
        val username = prefManager.getUsername()
        if (!username.isNullOrEmpty()) {
            binding.tvWelcome.text = "Hai $username, mau baca buku apa hari ini?"
        }
    }

    private fun loadDataFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {

            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    allAuthorsList = response.body()?.data ?: emptyList()
                    rekomendasiAdapter.updateAuthors(allAuthorsList)
                    populerAdapter.updateAuthors(allAuthorsList)
                    getBooksFromApi()
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                if (_binding != null) binding.progressBar.visibility = View.GONE
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
                    Log.e("HomeFragment", "Books API Failure: ${t.message}")
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
                // Mencari nama penulis berdasarkan ID untuk keperluan filter pencarian
                val authorName = allAuthorsList.find { it.id == book.penulisId }?.namaPenulis?.lowercase() ?: ""
                
                // Cek apakah Judul ATAU Penulis mengandung kata kunci
                book.judulBuku.lowercase().contains(query) || authorName.contains(query)
            }
        }
        updateDisplay(filteredList)
    }

    private fun setupRecyclerView() {

        rekomendasiAdapter = BookAdapter(emptyList(), emptyList()) { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_homeFragment_to_detailbookFragment, bundle)
        }
        populerAdapter = BookAdapter(emptyList(), emptyList()) { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_homeFragment_to_detailbookFragment, bundle)
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

    private fun updateDisplay(listBuku: List<BookItem>) {
        rekomendasiAdapter.submitList(listBuku.take(5))
        populerAdapter.submitList(listBuku.drop(5))
    }

    private fun getGenreFromApi() {
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val genres = response.body()?.data ?: emptyList()
                    binding.chipGroupCategory.removeAllViews()
                    for (genre in genres.take(6)) {
                        addChipToGroup(genre.id, genre.namaGenre)
                    }
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                Log.e("HomeFragment", "Genre API Failure: ${t.message}")
            }
        })
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