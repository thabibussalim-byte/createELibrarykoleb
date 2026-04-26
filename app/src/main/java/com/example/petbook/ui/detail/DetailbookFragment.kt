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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            currentBook = BundleCompat.getParcelable(it, "book", BookItem::class.java)
            currentWriter = it.getString("book_writer")
            currentPublisher = it.getString("book_publisher")
            currentGenre = it.getString("book_genre")
        }

        currentBook?.let { displayBookDetail(it, currentWriter, currentPublisher, currentGenre) }

        binding.btnPinjam.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.detailbookFragment) {

                val bundle = Bundle().apply {
                    putParcelable("book", currentBook)
                    putString("book_writer", currentWriter)
                    putString("book_publisher", currentPublisher)
                }

                // Gunakan navigasi yang aman
                findNavController().navigate(
                    R.id.action_detailBookFragment_to_detailpeminjamanFragment,
                    bundle
                )
            }
        }
    }


    private fun displayBookDetail(book: BookItem, writerName: String?, publisherName: String?, genreName: String?) {
        binding.apply {
            tvDetailTitle.text = book.judulBuku
            tvDetailDescription.text = book.deskripsi
            tvDetailStock.text = book.stok.toString()
            tvDetailAuthor.text = writerName ?: "Penulis: ${book.penulisId}"
            tvDetailPublisher.text = publisherName ?: "Penerbit: ${book.penerbitId}"
            tvDetailTglTerbit.text = book.tglTerbit
            tvDetailGenreLabel.text = genreName ?: "Umum"

            Glide.with(requireContext())
                .load(book.foto)
                .placeholder(R.drawable.bintang)
                .error(R.drawable.bintang)
                .into(ivDetailCover)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}