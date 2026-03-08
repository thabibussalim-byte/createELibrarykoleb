package com.example.elibrarypetik.ui.home

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
import com.example.elibrarypetik.data.model.Book
import com.example.elibrarypetik.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
                        binding.chipGroupCategory.removeAllViews()
                        
                        // Menampilkan 6 genre pertama secara bersih tanpa "Semua" atau "Lainnya"
                        val limitedGenres = listGenre.take(6)
                        for (genre in limitedGenres) {
                            addChipToGroup(genre.namaGenre, false) {
                                Toast.makeText(requireContext(), "Melihat kategori: ${genre.namaGenre}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                Log.e("HomeFragment", "API Failure: ${t.message}")
            }
        })
    }

    private fun addChipToGroup(name: String, isSelected: Boolean, onClick: () -> Unit) {
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isSelected
        chip.setOnClickListener { onClick() }
        binding.chipGroupCategory.addView(chip)
    }

    private fun setupRecyclerView() {
        val dummyBooks = listOf(
            Book(1, "Bumi", "Tere Liye", "https://picsum.photos/id/1/200/300", 4.5f),
            Book(2, "Hujan", "Tere Liye", "https://picsum.photos/id/10/200/300", 4.8f),
            Book(3, "Selena", "Tere Liye", "https://picsum.photos/id/20/200/300", 4.2f),
            Book(4, "Gadis Kretek", "Ratih Kumala", "https://picsum.photos/id/30/200/300", 4.7f)
        )

        val bookAdapter = BookAdapter(dummyBooks)

        binding.rvRekomendasi.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = bookAdapter
            setHasFixedSize(true)
        }

        binding.rvPopuler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}