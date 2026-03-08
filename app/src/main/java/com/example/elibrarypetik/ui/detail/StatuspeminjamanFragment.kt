package com.example.elibrarypetik.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.FragmentStatuspeminjamanBinding

class StatuspeminjamanFragment : Fragment() {

    private var _binding: FragmentStatuspeminjamanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatuspeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Simulasi data status dari API (Nanti diganti dengan data asli)
        val currentStatus = "terlambat" // Coba ganti ke: "proses", "dipinjam", "terlambat"
        updateStatusUI(currentStatus)
    }

    private fun updateStatusUI(status: String) {
        binding.tvStatusBadge.text = status.uppercase()

        when (status.lowercase()) {
            "dipinjam" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                binding.tvStatusDenda.text = "Denda: Rp 0"
                binding.tvStatusDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
                binding.btnPerpanjang.visibility = View.VISIBLE
            }
            "terlambat" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvStatusDenda.text = "Denda: Rp 6.000 (Telat 3 hari)"
                binding.tvStatusDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                // Biasanya buku telat tidak bisa diperpanjang
                binding.btnPerpanjang.visibility = View.GONE 
            }
            "proses" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvStatusDenda.text = "Permohonan sedang ditinjau"
                binding.btnPerpanjang.visibility = View.GONE
                binding.btnKembalikan.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}