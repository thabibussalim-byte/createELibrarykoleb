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
        // Data Dummy Riwayat
        val dummyHistory = listOf(
            HistoryItem(1, "Buya Hamka", "A. Fuadi", "https://i.pinimg.com/1200x/e4/93/95/e4939592075ca1824c40075548a069f0.jpg", "1 Januari - 5 Januari 2026"),
            HistoryItem(2, "Senja di Jakarta", "Mochtar Lubis", "https://i.pinimg.com/736x/56/9f/15/569f1519ff55cca588f3524ed9697324.jpg", "10 Januari - 15 Januari 2026"),
            HistoryItem(3, "Dilan 1990", "Pidi Baiq", "https://i.pinimg.com/736x/48/5f/12/485f1211ebb34c999522de421c63849b.jpg", "Denda: Rp 2.000 (Telat 2 hari)", "Rp 2.000", true),
            HistoryItem(4, "Negeri Para Bedebah", "Tere Liye", "https://i.pinimg.com/736x/de/be/49/debe492d980f7141c1a32ac93f49bb77.jpg", "Buku Ditolak")
        )

        val historyAdapter = HistoryAdapter(dummyHistory) { item ->
            // Navigasi ke Detail History saat item diklik
            findNavController().navigate(R.id.action_historyFragment_to_detailHistoryFragment)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = historyAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}