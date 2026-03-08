package com.example.elibrarypetik.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.model.HistoryItem
import com.example.elibrarypetik.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Mengisi Data Dummy
        val dummyHistory = listOf(
            HistoryItem(1, "Buya Hamka", "A. Fuadi", "https://picsum.photos/id/1/200/300", "1 Jan 2026", "8 Jan 2026", "Dipinjam"),
            HistoryItem(2, "Dilan 1990", "Pidi Baiq", "https://picsum.photos/id/10/200/300", "20 Des 2025", "27 Des 2025", "Terlambat", "Rp 2.000", true),
            HistoryItem(3, "Bumi", "Tere Liye", "https://picsum.photos/id/20/200/300", "15 Nov 2025", "22 Nov 2025", "Selesai")
        )

        val historyAdapter = HistoryAdapter(dummyHistory) { item ->
            // Alur yang Benar: Dari History ke Detail History
            findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}