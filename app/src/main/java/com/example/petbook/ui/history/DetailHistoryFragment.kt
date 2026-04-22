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
        binding.tvHelp.setOnClickListener {
            navigateToDetail(
                "Bagaimana cara mengembalikan buku?",
                "Bawa buku fisik ke petugas perpustakaan. Setelah petugas memproses pengembalian, status di aplikasi Anda akan otomatis berubah menjadi 'Dikembalikan'."
            )
        }
    }
    private fun navigateToDetail(title: String, answer: String) {
        val bundle = Bundle().apply {
            putString("faq_title", title)
            putString("faq_answer", answer)
        }
        findNavController().navigate(R.id.action_detailHistoryFragment_to_faqDetailFragment, bundle)
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

    private fun updateDetailUI(status: String) {
        binding.tvDetailHistoryStatusBadge.text = status.uppercase()

        when (status.lowercase()) {
            "dipinjam" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                binding.tvDetailStatus.text = "Status: Aktif"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
                binding.tvHelp.visibility = View.VISIBLE
            }
            "pending" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvDetailHistoryStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvDetailStatus.text = "Menunggu dikonfirmasi admin"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.tvHelp.visibility = View.GONE
            }
            "dikembalikan"-> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                binding.tvDetailHistoryStatusBadge.text = "SELESAI"
                binding.tvHelp.visibility = View.GONE

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
                binding.tvHelp.visibility = View.GONE
            }
            else -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvDetailStatus.text = "Status: $status"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.tvHelp.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}