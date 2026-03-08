package com.example.elibrarypetik.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.FragmentDetailHistoryBinding

class DetailHistoryFragment : Fragment() {

    private var _binding: FragmentDetailHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Simulasi status buku yang diklik (Nanti dikirim lewat SafeArgs/Bundle)
        val statusBuku = "dipinjam" // Coba ganti: "terlambat", "selesai", "proses", "ditolak"
        
        updateDetailUI(statusBuku)
    }

    private fun updateDetailUI(status: String) {
        binding.tvDetailHistoryStatusBadge.text = status.uppercase()

        when (status.lowercase()) {
            "dipinjam" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                binding.tvDetailHistoryDenda.text = "Denda: Rp 0"
                binding.tvDetailHistoryDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
                binding.layoutActions.visibility = View.VISIBLE
                binding.btnPerpanjangHistory.visibility = View.VISIBLE
            }
            "terlambat" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvDetailHistoryDenda.text = "Denda: Rp 6.000 (Telat 3 hari)"
                binding.tvDetailHistoryDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                binding.layoutActions.visibility = View.VISIBLE
                binding.btnPerpanjangHistory.visibility = View.GONE // Telat tidak bisa perpanjang
            }
            "proses" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvDetailHistoryStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvDetailHistoryDenda.text = "Permohonan sedang ditinjau"
                binding.layoutActions.visibility = View.GONE // Belum ada aksi yang bisa dilakukan
            }
            "ditolak" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_ditolak)
                binding.tvDetailHistoryDenda.text = "Pengajuan ditolak oleh admin"
                binding.tvDetailHistoryDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                binding.layoutActions.visibility = View.GONE
            }
            "selesai" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                binding.tvDetailHistoryStatusBadge.text = "DIKEMBALIKAN"
                binding.tvDetailHistoryDenda.text = "Buku sudah dikembalikan tepat waktu"
                binding.layoutActions.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}