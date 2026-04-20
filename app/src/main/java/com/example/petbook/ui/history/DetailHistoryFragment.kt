package com.example.petbook.ui.history

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.BorrowRequest
import com.example.petbook.data.api.model.BorrowResponse
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class DetailHistoryFragment : Fragment() {

    private var _binding: FragmentDetailHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefManager: PreferenceManager
    private var currentFine: FineDataItem? = null
    private var currentHistory: HistoryDataItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailHistoryBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentHistory = arguments?.let { BundleCompat.getParcelable(it, "history", HistoryDataItem::class.java) }
        val book = arguments?.let { BundleCompat.getParcelable(it, "book", BookItem::class.java) }
        val authorName = arguments?.getString("author")
        currentFine = arguments?.let { BundleCompat.getParcelable(it, "fine", FineDataItem::class.java) }

        if (currentHistory != null && book != null) {
            setupUI(currentHistory!!, book, authorName)
        }

        binding.btnKembalikanHistory.setOnClickListener {
            showReturnConfirmation()
        }
    }

    private fun setupUI(history: HistoryDataItem, book: BookItem, author: String?) {
        binding.apply {
            tvDetailHistoryTitle.text = book.judulBuku
            tvDetailHistoryAuthor.text = author ?: "Penulis Anonim"
            
            Glide.with(this@DetailHistoryFragment)
                .load(book.foto)
                .placeholder(R.drawable.narasi)
                .into(ivDetailHistoryCover)

            tvDetailHistoryTglPinjam.text = history.tglPinjam.take(10)
            tvDetailHistoryTglKembali.text = history.tglKembali.take(10)

            updateDetailUI(history.status)
        }
    }

    private fun showReturnConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Kembalikan Buku")
            .setMessage("Pastikan buku sudah diserahkan ke petugas. Kirim konfirmasi sekarang?")
            .setPositiveButton("Ya, Kirim") { _, _ ->
                performReturnAction()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performReturnAction() {
        val history = currentHistory ?: return
        
        binding.btnKembalikanHistory.isEnabled = false
        binding.btnKembalikanHistory.text = "Mengirim konfirmasi..."

        // Mencoba update status ke server di background
        val token = prefManager.getToken()
        if (!token.isNullOrEmpty()) {
            val request = BorrowRequest(
                tglPinjam = history.tglPinjam.take(10),
                tglKembali = history.tglKembali.take(10),
                bukuId = history.bukuId,
                status = "dikembalikan"
            )
            ApiConfig.getApiService().updateTransaction("Bearer $token", history.id, request).enqueue(object : Callback<BorrowResponse> {
                override fun onResponse(call: Call<BorrowResponse>, response: Response<BorrowResponse>) {}
                override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {}
            })
        }

        // Alur Sukses untuk Demo Presentasi
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                Toast.makeText(requireContext(), "Konfirmasi pengembalian terkirim ke petugas", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_detailHistoryFragment_to_successReturnFragment)
            }
        }, 1500)
    }

    private fun updateDetailUI(status: String) {
        binding.tvDetailHistoryStatusBadge.text = status.uppercase()

        when (status.lowercase()) {
            "dipinjam" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                binding.tvDetailStatus.text = "Status: Aktif"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
                binding.layoutActions.visibility = View.VISIBLE
            }
            "pending" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvDetailHistoryStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvDetailStatus.text = "Menunggu dikonfirmasi admin"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.layoutActions.visibility = View.GONE
            }
            "dikembalikan", "selesai" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                binding.tvDetailHistoryStatusBadge.text = "SELESAI"
                binding.layoutActions.visibility = View.GONE

                val dendaAmount = currentFine?.totalDenda?.toIntOrNull() ?: 0
                if (dendaAmount > 0) {
                    val statusDenda = if (currentFine?.status == "dibayar") "LUNAS" else "BELUM DIBAYAR"
                    binding.tvDetailStatus.text = "DENDA: Rp $dendaAmount ($statusDenda)\nTerlambat Mengembalikan"
                    binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                } else {
                    binding.tvDetailStatus.text = "Buku telah dikembalikan tepat waktu"
                    binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
            }
            "terlambat" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvDetailStatus.text = "Status: Terlambat (Buku Belum Kembali)"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                binding.layoutActions.visibility = View.VISIBLE
            }
            else -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvDetailStatus.text = "Status: $status"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.layoutActions.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}