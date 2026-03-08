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
        // Mengisi Data Dummy sesuai dengan struktur HistoryItem baru
        val dummyHistory = listOf(
            HistoryItem(
                id = 1,
                title = "Buya Hamka",
                author = "A. Fuadi",
                imageUrl = "https://picsum.photos/id/1/200/300",
                borrowDate = "1 Jan 2026",
                dueDate = "8 Jan 2026",
                status = "Dipinjam"
            ),
            HistoryItem(
                id = 2,
                title = "Dilan 1990",
                author = "Pidi Baiq",
                imageUrl = "https://picsum.photos/id/10/200/300",
                borrowDate = "20 Des 2025",
                dueDate = "27 Des 2025",
                status = "Terlambat",
                fine = "Rp 2.000",
                isLate = true
            ),
            HistoryItem(
                id = 3,
                title = "Bumi",
                author = "Tere Liye",
                imageUrl = "https://picsum.photos/id/20/200/300",
                borrowDate = "15 Nov 2025",
                dueDate = "22 Nov 2025",
                status = "Selesai"
            )
        )

        val historyAdapter = HistoryAdapter(dummyHistory) { item ->
            // Pindah ke Detail History saat item diklik
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