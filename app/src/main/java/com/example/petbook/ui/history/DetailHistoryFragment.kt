package com.example.petbook.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailHistoryBinding
import androidx.core.graphics.toColorInt

class DetailHistoryFragment : Fragment() {
    private var _binding: FragmentDetailHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PreferenceManager
    private var currentHistory: HistoryDataItem? = null
    private var currentBook: BookItem? = null
    private var currentFine: FineDataItem? = null

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
        currentBook = arguments?.let { BundleCompat.getParcelable(it, "book", BookItem::class.java) }
        currentFine = arguments?.let { BundleCompat.getParcelable(it, "fine", FineDataItem::class.java) }

        val writer = arguments?.getString("book_writer") ?: arguments?.getString("author") ?: "Penulis Anonim"
        val publisher = arguments?.getString("book_publisher") ?: "Penerbit Anonim"
        val genre = arguments?.getString("book_genre") ?: "Umum"
        val rating = arguments?.getFloat("book_rating") ?: 0.0f

        if (currentHistory != null && currentBook != null) {
            setupUI(currentHistory!!, currentBook!!, writer, publisher, genre)
            checkAndShowSuccessFragment(currentHistory!!, currentBook!!)
        }

        binding.llHistory.setOnClickListener {
            currentBook?.let { book ->
                val bundle = Bundle().apply {
                    putParcelable("book", book)
                    putString("book_writer", writer)
                    putString("book_publisher", publisher)
                    putString("book_genre", genre)
                    putFloat("book_rating", rating)
                }
                // Navigasi ke Detail Buku
                findNavController().navigate(R.id.action_detailHistoryFragment_to_detailbookFragment, bundle)
            }
        }
    }

    private fun checkAndShowSuccessFragment(history: HistoryDataItem, book: BookItem) {
        val status = history.status.lowercase()
        if ((status == "dipinjam" || status == "dikembalikan" || status == "selesai") &&
            !prefManager.isStatusSeen(history.id, status)) {

            prefManager.setStatusSeen(history.id, status)

            val bundle = Bundle().apply {
                putString("book_title", book.judulBuku)
                putString("status", status)
            }
            findNavController().navigate(R.id.successReturnFragment, bundle)
        }
    }

    private fun setupUI(
        history: HistoryDataItem,
        book: BookItem,
        author: String,
        publisher: String,
        genre: String
    ) {
        binding.apply {
            tvDetailHistoryTitle.text = book.judulBuku
            tvDetailHistoryAuthor.text = "Penulis: $author"

            Glide.with(this@DetailHistoryFragment)
                .load(book.foto)
                .placeholder(R.drawable.narasi)
                .into(ivDetailHistoryCover)

            tvDetailHistoryTglPinjam.text = history.tglPinjam.take(10)
            tvDetailHistoryTglKembali.text = history.tglKembali.take(10)

            updateDetailUI(history)
        }
        
        binding.tvHelp.setOnClickListener {
            val bundle = Bundle().apply {
                putString("faq_title", "Bagaimana cara mengembalikan buku?")
                putString("faq_answer", "Bawa buku fisik ke petugas perpustakaan. Setelah petugas memproses pengembalian, status di aplikasi Anda akan otomatis berubah menjadi 'Dikembalikan'.")
            }
            // Pastikan menggunakan ID aksi yang benar sesuai nav_main.xml
            findNavController().navigate(R.id.action_detailHistoryFragment_to_faqDetailFragment, bundle)
        }
    }

    private fun updateDetailUI(history: HistoryDataItem) {
        if (_binding == null) return
        val status = history.status.lowercase()

        when (status) {
            "dipinjam" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                binding.tvDetailHistoryStatusBadge.setTextColor("#1D4ED8".toColorInt())
                binding.tvDetailHistoryStatusBadge.text = "SEDANG DIPINJAM"
                binding.tvDetailHistoryStatusBadge2.visibility = View.GONE
                binding.tvHelp.visibility = View.VISIBLE
                binding.tvDetailStatus.text = "Status: Aktif"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
            }

            "pending" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvDetailHistoryStatusBadge.setTextColor("#C2410C".toColorInt())
                binding.tvDetailHistoryStatusBadge2.visibility = View.GONE
                binding.tvHelp.visibility = View.GONE
                binding.tvDetailHistoryStatusBadge.text = "MENUNGGU PERSETUJUAN"
                binding.tvDetailStatus.text = "Menunggu dikonfirmasi admin"
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            "dikembalikan", "selesai" -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dikembalikan)
                binding.tvDetailHistoryStatusBadge.setTextColor("#047857".toColorInt())
                binding.tvDetailHistoryStatusBadge2.visibility = View.GONE
                binding.tvHelp.visibility = View.GONE
                binding.tvDetailHistoryStatusBadge.text = "SELESAI"

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

            else -> {
                binding.tvDetailHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                binding.tvDetailHistoryStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                binding.tvDetailHistoryStatusBadge.text = "TERLAMBAT"
                binding.tvDetailStatus.text = "Status: Terlambat (Buku Belum Kembali)"
                binding.tvDetailHistoryStatusBadge2.visibility = View.GONE
                binding.tvHelp.visibility = View.VISIBLE
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
