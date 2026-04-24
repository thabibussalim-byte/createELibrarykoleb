package com.example.petbook.ui.history

import android.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailHistoryFragment : Fragment() {
    private var _binding: FragmentDetailHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefManager: PreferenceManager
    private var currentFine: FineDataItem? = null
    private var currentHistory: HistoryDataItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

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

            // Cek apakah terlambat secara otomatis sebelum update UI
            val finalStatus = if (history.status.lowercase() == "dipinjam" && isOverdue(history.tglKembali)) {
                "terlambat"
            } else {
                history.status
            }

            updateDetailUI(finalStatus)
        }
    }

    // Fungsi sakti untuk mengecek apakah sudah melewati jatuh tempo
    private fun isOverdue(dueDateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dueDate = sdf.parse(dueDateStr.take(10))
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            dueDate != null && today.after(dueDate)
        } catch (e: Exception) {
            false
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

        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                Toast.makeText(requireContext(), "Konfirmasi pengembalian terkirim ke petugas", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_detailHistoryFragment_to_successReturnFragment)
            }
        }, 1500)
    }

    private fun updateDetailUI(status: String) {
        val badge = binding.tvDetailHistoryStatusBadge
        badge.text = status.uppercase()

        when (status.lowercase()) {
            "dipinjam" -> {
                badge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                badge.setTextColor(Color.parseColor("#1E40AF")) 
                binding.tvDetailStatus.text = "Status: Aktif"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
                binding.layoutActions.visibility = View.VISIBLE
            }
            "pending" -> {
                badge.setBackgroundResource(R.drawable.bg_status_pending)
                badge.setTextColor(Color.parseColor("#9A3412")) 
                badge.text = "PENDING"
                binding.tvDetailStatus.text = "Menunggu dikonfirmasi admin"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.layoutActions.visibility = View.GONE
            }
            "dikembalikan", "selesai" -> {
                badge.setBackgroundResource(R.drawable.bg_status_dikembalikan)
                badge.setTextColor(Color.parseColor("#065F46")) 
                badge.text = "SELESAI"
                binding.layoutActions.visibility = View.GONE

                val dendaAmount = currentFine?.totalDenda?.filter { it.isDigit() }?.toIntOrNull() ?: 0
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
                badge.setBackgroundResource(R.drawable.bg_status_telat)
                badge.setTextColor(Color.parseColor("#991B1B")) // Merah Tua
                badge.text = "TERLAMBAT"
                binding.tvDetailStatus.text = "Status: Terlambat (Buku Belum Kembali)"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                binding.layoutActions.visibility = View.VISIBLE
            }
            else -> {
                badge.setBackgroundResource(R.drawable.bg_status_telat)
                badge.setTextColor(Color.parseColor("#991B1B"))
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
