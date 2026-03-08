package com.example.elibrarypetik.ui.buku

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elibrarypetik.data.api.ApiConfig
import com.example.elibrarypetik.data.api.model.GenreResponse
import com.example.elibrarypetik.data.model.HistoryItem
import com.example.elibrarypetik.databinding.FragmentBookBinding
import com.example.elibrarypetik.ui.history.HistoryAdapter
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
                if (response.isSuccessful && response.body() != null) {
                    val genres = response.body()?.data
                    genres?.let { listGenre ->
                        binding.chipGroupBookFilter.removeAllViews()
                        
                        // Tambahkan Chip "Semua"
                        addChipToGroup("Semua", true)
                        
                        // Tambahkan Chip dari API
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
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isSelected
        
        chip.setOnClickListener {
            Toast.makeText(requireContext(), "Filter: $name", Toast.LENGTH_SHORT).show()
            // Nanti di sini panggil API filter buku
        }
        
        binding.chipGroupBookFilter.addView(chip)
    }

    private fun setupRecyclerView() {
        // Data Dummy Katalog (Bisa gunakan HistoryItem model untuk tampilan list samping)
        val dummyBooks = listOf(
            HistoryItem(1, "Bumi", "Tere Liye", "https://picsum.photos/id/1/200/300", "Tersedia"),
            HistoryItem(2, "Hujan", "Tere Liye", "https://picsum.photos/id/10/200/300", "Tersedia"),
            HistoryItem(3, "Selena", "Tere Liye", "https://picsum.photos/id/20/200/300", "Dipinjam"),
            HistoryItem(4, "Gadis Kretek", "Ratih Kumala", "https://picsum.photos/id/30/200/300", "Tersedia")
        )

        // Gunakan HistoryAdapter (karena layout item_history sangat cocok untuk katalog list)
        val bookAdapter = HistoryAdapter(dummyBooks) { item ->
            Toast.makeText(requireContext(), "Klik: ${item.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvAllBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}