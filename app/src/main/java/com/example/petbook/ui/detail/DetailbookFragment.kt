package com.example.petbook.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.databinding.FragmentDetailbookBinding

class DetailbookFragment : Fragment() {

    private var _binding: FragmentDetailbookBinding? = null
    private val binding get() = _binding!!

    private var currentBook: BookItem? = null
    private var currentWriter: String? = null
    private var currentPublisher: String? = null
    private var currentGenre: String? = null
    private var currentRating: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data dari arguments
        currentBook = arguments?.let {
            BundleCompat.getParcelable(it, "book", BookItem::class.java)
        }
        currentWriter = arguments?.getString("book_writer")
        currentPublisher = arguments?.getString("book_publisher")
        currentGenre = arguments?.getString("book_genre")
        currentRating = arguments?.getFloat("book_rating", 0f) ?: 0f
        
        currentBook?.let { displayBookDetail(it, currentWriter, currentPublisher, currentGenre, currentRating) }

        // Klik tombol pinjam untuk pindah ke formulir peminjaman
        binding.btnPinjam.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("book", currentBook)
                putString("book_writer", currentWriter)
                putString("book_publisher", currentPublisher)
            }
            findNavController().navigate(R.id.action_detailbookFragment_to_detailpeminjamanFragment, bundle)
        }
    }

    private fun displayBookDetail(book: BookItem, writerName: String?, publisherName: String?, genreName: String?, rating: Float) {
        binding.apply {
            tvDetailTitle.text = book.judulBuku
            tvDetailDescription.text = book.deskripsi
            tvDetailStock.text = book.stok.toString()
            
            tvDetailAuthor.text = writerName ?: "Penulis: ${book.penulisId}"
            tvDetailPublisher.text = publisherName ?: "Penerbit: ${book.penerbitId}"
            
            // Set Teks Rating agar SINKRON
            tvDetailRating.text = String.format("%.1f", rating)

            // Set Genre Text
            tvDetailGenreLabel.text = genreName ?: "Umum"

            Glide.with(requireContext())
                .load(book.foto)
                .placeholder(R.drawable.bintang)
                .into(ivDetailCover)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}