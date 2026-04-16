package com.example.elibrarypetik.ui.buku

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.api.ApiConfig
import com.example.elibrarypetik.data.api.model.BookItem
import com.example.elibrarypetik.data.api.model.BookResponse
import com.example.elibrarypetik.data.api.model.GenreResponse
import com.example.elibrarypetik.databinding.FragmentBookBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookKatalogAdapter: BookKatalogAdapter
    
    // Simpan semua data buku untuk filter
    private var allBooks: List<BookItem> = emptyList()

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
        getGenreFromApi()
        getBooksFromApi()
    }

    private fun setupRecyclerView() {
        bookKatalogAdapter = BookKatalogAdapter(emptyList()) { book ->
            val bundle = Bundle().apply {
                putInt("book_id", book.id)
                putString("book_title", book.judulBuku)
            }
            findNavController().navigate(R.id.action_bookFragment_to_detailbookFragment, bundle)
        }

        binding.rvAllBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookKatalogAdapter
        }
    }

    private fun getBooksFromApi() {
        binding.progressBarBook.visibility = View.VISIBLE
        ApiConfig.getApiService().getBooks().enqueue(object : Callback<BookResponse> {
            override fun onResponse(call: Call<BookResponse>, response: Response<BookResponse>) {
                if (_binding != null) {
                    binding.progressBarBook.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        allBooks = response.body()?.data ?: emptyList()
                        bookKatalogAdapter.updateData(allBooks)
                    }
                }
            }

            override fun onFailure(call: Call<BookResponse>, t: Throwable) {
                if (_binding != null) {
                    binding.progressBarBook.visibility = View.GONE
                    Log.e("BookFragment", "Error API Buku: ${t.message}")
                }
            }
        })
    }

    private fun getGenreFromApi() {
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val genres = response.body()?.data
                    genres?.let { listGenre ->
                        binding.chipGroupBookFilter.removeAllViews()
                        
                        // 1. Tambahkan filter "Semua" di awal
                        addChipToGroup(-1, "Semua", true)
                        
                        // 2. Tambahkan SEMUA genre dari API
                        for (genre in listGenre) {
                            addChipToGroup(genre.id, genre.namaGenre, false)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                Log.e("BookFragment", "Error API Genre: ${t.message}")
            }
        })
    }

    private fun addChipToGroup(genreId: Int, name: String, isDefault: Boolean) {
        if (_binding == null) return
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isDefault
        
        chip.setOnClickListener {
            if (genreId == -1) {
                // Jika klik "Semua"
                bookKatalogAdapter.updateData(allBooks)
                binding.tvStatusLabel.text = "Menampilkan semua koleksi"
            } else {
                // Filter berdasarkan genreId
                val filtered = allBooks.filter { it.genreId == genreId }

                bookKatalogAdapter.updateData(filtered)
                binding.tvStatusLabel.text = "Kategori: $name (${filtered.size} buku)"
            }
        }
        
        binding.chipGroupBookFilter.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}