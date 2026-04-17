package com.example.petbook.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.data.model.HistoryItem
import com.example.petbook.databinding.FragmentDetailDendaBinding
import com.example.petbook.ui.history.HistoryAdapter

class DetailDendaFragment : Fragment() {

    private var _binding: FragmentDetailDendaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailDendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDendaList()
    }

    private fun setupDendaList() {
        // Simulasi data buku yang kena denda
        val dendaBooks = listOf(
            HistoryItem(1, "Buya Hamka", "A. Fuadi", "https://picsum.photos/id/1/200/300", "1 Jan 2026", "8 Jan 2026", "Terlambat", "Rp 2.000", true)
        )

        binding.tvDetailTotalDenda.text = "Rp: 2000"
        
        // PERBAIKAN: Ubah format teks agar sesuai dengan keinginan Anda (Dinamis)
        binding.tvJumlahBukuDenda.text = "${dendaBooks.size} Buku Telat"

        val historyAdapter = HistoryAdapter(dendaBooks) {
            // Logika klik jika diperlukan
        }

        binding.rvBukuDenda.apply {
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