package com.example.elibrarypetik.ui.home

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
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val genres = response.body()?.data
                    genres?.let { listGenre ->
                        binding.chipGroupCategory.removeAllViews()
                        val limitedGenres = listGenre.take(6)
                        for (genre in limitedGenres) {
                            addChipToGroup(genre.namaGenre, false) {

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
        if (_binding == null) return
        val chip = Chip(requireContext())
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = isSelected
        chip.setOnClickListener { onClick() }
        binding.chipGroupCategory.addView(chip)
    }

    private fun setupRecyclerView() {
        val dummyBooks = listOf(
            Book(1, "Bumi", "Tere Liye", "https://i.pinimg.com/736x/b9/70/f9/b970f956854a01648c0aca3cae176c84.jpg", 4.5f),
            Book(2, "They Didn't Know", "Tuhina Pal", "https://i.pinimg.com/736x/4e/41/4b/4e414baf3c8c902bcdcae73d27d8037f.jpg", 4.8f),
            Book(3, "Sabtu Bersama Bapak", "Ratih Kumala", "https://i.pinimg.com/736x/a3/06/28/a3062830e71e97c86b0e45a0e997baf1.jpg", 4.2f),
            Book(4, "EGO IS THE ENEMY", "Ryan Holiday", "https://i.pinimg.com/736x/50/ed/cb/50edcbcf09d2789d041f089a45507cba.jpg0", 4.7f)
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