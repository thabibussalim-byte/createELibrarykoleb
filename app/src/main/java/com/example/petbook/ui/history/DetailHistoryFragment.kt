package com.example.petbook.ui.history

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.R
import com.example.petbook.databinding.FragmentDetailHistoryBinding

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

        // Simulasi status buku (Nanti dikirim lewat SafeArgs)
        val statusBuku = "dipinjam" 
        updateDetailUI(statusBuku)

        binding.btnKembalikanHistory.setOnClickListener {
            showReturnConfirmation()
        }

        binding.btnPerpanjangHistory.setOnClickListener {
            showExtensionDialog()
        }
    }

    private fun showReturnConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Kembalikan Buku")
            .setMessage("Apakah Anda yakin ingin mengembalikan buku ini sekarang?")
            .setPositiveButton("Ya, Kembalikan") { _, _ ->
                Toast.makeText(requireContext(), "Buku berhasil dikembalikan!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showExtensionDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Perpanjang Peminjaman")
        builder.setMessage("Berapa hari Anda ingin menambah masa pinjam?")

        // Membuat input field di dalam dialog
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Contoh: 7"
        
        val container = LinearLayout(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 0, 60, 0)
        input.layoutParams = params
        container.addView(input)
        
        builder.setView(container)

        builder.setPositiveButton("Ajukan") { _, _ ->
            val days = input.text.toString()
            if (days.isNotEmpty()) {
                Toast.makeText(context, "Permintaan perpanjangan $days hari diajukan!", Toast.LENGTH_SHORT).show()
                // Ubah status ke proses karena butuh persetujuan admin
                updateDetailUI("proses")
            } else {
                Toast.makeText(context, "Harap isi jumlah hari", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
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
                binding.btnPerpanjangHistory.visibility = View.GONE
            }
            "proses" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvDetailHistoryStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvDetailHistoryDenda.text = "Permohonan sedang ditinjau"
                binding.tvDetailHistoryDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.layoutActions.visibility = View.GONE
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
                binding.tvDetailHistoryDenda.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.layoutActions.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}