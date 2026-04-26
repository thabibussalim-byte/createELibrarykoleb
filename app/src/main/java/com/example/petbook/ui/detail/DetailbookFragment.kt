package com.example.petbook.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.databinding.FragmentDetailbookBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // 1. Ambil data dari arguments secara aman
        arguments?.let {
            currentBook = BundleCompat.getParcelable(it, "book", BookItem::class.java)
            currentWriter = it.getString("book_writer")
            currentPublisher = it.getString("book_publisher")
            currentGenre = it.getString("book_genre")
        }

        currentBook?.let { displayBookDetail(it, currentWriter, currentPublisher, currentGenre) }

        // 2. Listener tombol Pinjam dengan pengamanan
        binding.btnPinjam.setOnClickListener {
            // Cek apakah NavController masih berada di destinasi ini untuk mencegah crash "Navigation action cannot be found"
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

            // Tampilkan tanggal terbit
            tvDetailTglTerbit.text = book.tglTerbit

            tvDetailGenreLabel.text = genreName ?: "Umum"

            Glide.with(requireContext())
                .load(book.foto)
                .placeholder(R.drawable.bintang) // Ganti dengan drawable loading yang sesuai
                .error(R.drawable.bintang)      // Ganti dengan drawable error yang sesuai
                .into(ivDetailCover)
        }
    }

    // Contoh fungsi jika kamu perlu membersihkan database sebelum pindah (mengatasi error Main Thread)
    private fun clearDatabaseAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Lakukan operasi database di sini (Thread Background)
            // appDatabase.clearAllTables()

            withContext(Dispatchers.Main) {
                // Kembali ke Thread Utama untuk Navigasi
                if (findNavController().currentDestination?.id == R.id.detailbookFragment) {
                    findNavController().navigate(R.id.action_detailBookFragment_to_detailpeminjamanFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}