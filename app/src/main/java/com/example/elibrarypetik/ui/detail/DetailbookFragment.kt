package com.example.elibrarypetik.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.api.model.BookItem
import com.example.elibrarypetik.databinding.FragmentDetailbookBinding

class DetailbookFragment : Fragment() {

    private var _binding: FragmentDetailbookBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data buku dari arguments menggunakan BundleCompat agar tidak deprecated
        val book = arguments?.let {
            BundleCompat.getParcelable(it, "book", BookItem::class.java)
        }
        
        book?.let { displayBookDetail(it) }

        // Klik tombol pinjam untuk pindah ke formulir peminjaman
        binding.btnPinjam.setOnClickListener {
            findNavController().navigate(R.id.action_detailbookFragment_to_detailpeminjamanFragment)
        }
    }

    private fun displayBookDetail(book: BookItem) {
        binding.apply {
            tvDetailTitle.text = book.judulBuku
            tvDetailDescription.text = book.deskripsi
            tvDetailStock.text = "${book.stok} Tersedia"
            
            // Note: Penulis dan Penerbit di model masih ID
            tvDetailAuthor.text = "ID Penulis: ${book.penulisId}"
            tvDetailPublisher.text = "ID Penerbit: ${book.penerbitId}"

            Glide.with(requireContext())
                .load(book.foto)
                .placeholder(R.drawable.bintang) // Tambahkan placeholder jika perlu
                .into(ivDetailCover)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}