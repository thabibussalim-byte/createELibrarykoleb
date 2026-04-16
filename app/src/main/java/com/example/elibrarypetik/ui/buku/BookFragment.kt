package com.example.elibrarypetik.ui.buku

import android.os.Bundle
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
import com.example.elibrarypetik.data.api.model.GenreItem
import com.example.elibrarypetik.data.api.model.GenreResponse
import com.example.elibrarypetik.data.api.model.PublisherItem
import com.example.elibrarypetik.data.api.model.PublisherResponse
import com.example.elibrarypetik.databinding.FragmentBookBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookKatalogAdapter: BookKatalogAdapter
    
    private var allBooks: List<BookItem> = emptyList()
    private var allPublishers: List<PublisherItem> = emptyList() // Simpan daftar penerbit
    private var allAuthors: List<AuthorItem> = emptyList() // Simpan daftar penulis
    private var allGenres: List<GenreItem> = emptyList() // Simpan daftar genre


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
        loadDataFromServer()
    }

    private fun loadDataFromServer() {
        binding.progressBarBook.visibility = View.VISIBLE
        
        // Fetch Authors
        ApiConfig.getApiService().getAuthors().enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val authors = response.body()?.data ?: emptyList()
                    bookKatalogAdapter.updateAuthors(authors)
                    fetchPublishers() // Lanjut ambil penerbit
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
                if (_binding != null && response.isSuccessful) {
                    allPublishers = response.body()?.data ?: emptyList()
                    getBooksFromApi() // Terakhir ambil buku
                }
            }
            override fun onFailure(call: Call<PublisherResponse>, t: Throwable) {
                if (_binding != null) getBooksFromApi()
            }
        })
    }

    private fun setupRecyclerView() {
        bookKatalogAdapter = BookKatalogAdapter(emptyList()) { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
                
                // Ambil nama penulis dari adapter
                val writerName = bookKatalogAdapter.getAuthorName(book.penulisId)
                putString("book_writer", writerName)

                
                // Ambil nama penerbit dari list lokal
                val publisherName = allPublishers.find { it.id == book.penerbitId }?.publisherName ?: "Penerbit Anonim"
                putString("book_publisher", publisherName)

                val genreName = allGenres.find { it.id == book.genreId }?.namaGenre ?: "Umum"
                putString("book_genre", genreName)

                val authorName = allAuthors.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
                putString("book_author", authorName)
            }
            findNavController().navigate(R.id.action_bookFragment_to_detailbookFragment, bundle)
        }

        binding.rvAllBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookKatalogAdapter
        }
    }

    private fun getBooksFromApi() {
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
                        addChipToGroup(-1, "Semua", true)
                        for (genre in listGenre) {
                            addChipToGroup(genre.id, genre.namaGenre, false)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {}
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
                bookKatalogAdapter.updateData(allBooks)
                binding.tvStatusLabel.text = "Menampilkan semua koleksi"
            } else {
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