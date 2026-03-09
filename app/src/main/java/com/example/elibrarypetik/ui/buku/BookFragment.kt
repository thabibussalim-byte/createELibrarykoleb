package com.example.elibrarypetik.ui.buku

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elibrarypetik.data.api.ApiConfig
import com.example.elibrarypetik.data.api.model.GenreResponse
import com.example.elibrarypetik.data.model.Book
import com.example.elibrarypetik.databinding.FragmentBookBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getGenreFromApi()
        setupRecyclerView()
    }

    private fun getGenreFromApi() {
        ApiConfig.getApiService().getGenres().enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                // PENGECEKAN PENTING: Pastikan binding tidak null sebelum akses UI
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val genres = response.body()?.data
                    genres?.let { listGenre ->
                        binding.chipGroupBookFilter.removeAllViews()
                        addChipToGroup("Semua", true)
                        for (genre in listGenre) {
                            addChipToGroup(genre.namaGenre, false)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                Log.e("BookFragment", "Error API: ${t.message}")
            }
        })
    }

    private fun addChipToGroup(name: String, isSelected: Boolean) {
        if (_binding == null) return
        
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isSelected
        chip.setOnClickListener {
            if (_binding != null) {
                if (name == "Semua") {
                    binding.tvStatusLabel.text = "Menampilkan semua koleksi"
                } else {
                    binding.tvStatusLabel.text = "Menampilkan kategori: $name"
                }
            }
        }
        binding.chipGroupBookFilter.addView(chip)
    }

    private fun setupRecyclerView() {
        val dummyBooks = listOf(
            Book(1, "Python for Beginners", "Paul Deitel", "https://i.pinimg.com/736x/b9/70/f9/b970f956854a01648c0aca3cae176c84.jpg", 4.5f),
            Book(2, "Computer Science", "Tere Liye", "https://i.pinimg.com/1200x/d3/a9/b5/d3a9b57d44fbc69ff9b52a32fb8a2d07.jpg", 4.8f),
            Book(3, "Selamat Tinggal", "Tere Liye", "https://i.pinimg.com/736x/6a/3b/13/6a3b1337ae7980bd8d36f09ba9bf4e8a.jpg", 4.2f),
            Book(4, "Bumi", "Tere Liye", "https://i.pinimg.com/736x/b9/70/f9/b970f956854a01648c0aca3cae176c84.jpg", 4.7f)
        )

        val bookAdapter = BookKatalogAdapter(dummyBooks) { book ->
            // Navigasi detail
        }

        binding.rvAllBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}