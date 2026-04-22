package com.example.petbook.ui.bantuan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.R
import com.example.petbook.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFaqClickListeners()
    }

    private fun setupFaqClickListeners() {
        // 1. Cara Meminjam
        binding.btnFaq1.setOnClickListener {
            navigateToDetail(
                "Bagaimana cara meminjam buku?",
                "Buka katalog buku, pilih buku yang Anda inginkan, lalu klik tombol 'Pinjam Buku'. Tunggu konfirmasi dari petugas melalui notifikasi aplikasi."
            )
        }

        // 2. Waktu Peminjaman
        binding.btnFaq2.setOnClickListener {
            navigateToDetail(
                "Berapa lama waktu peminjaman?",
                "Waktu peminjaman buku standarnya adalah 7 hari kalender sampai maksimal waktu peminjaman 14 hari kalender. Anda dapat melihat tanggal jatuh tempo pengembalian di menu Riwayat Peminjaman."
            )
        }

        // 3. Status Buku
        binding.btnFaq3.setOnClickListener {
            navigateToDetail(
                "Apa arti status dipinjam/ dikembalikan/ pending?",
                "Dipinjam: Buku sedang dalam genggaman Anda.\n Dikembalikan: buku yang telah selesai dibaca, lalu dikembalikan ke petugas. \nDipinjam: Buku yang sedang dipinjam oleh Anda."
            )
        }

        // 4. Cara Mengembalikan
        binding.btnFaq4.setOnClickListener {
            navigateToDetail(
                "Bagaimana cara mengembalikan buku?",
                "Bawa buku fisik ke petugas perpustakaan. Setelah petugas memproses pengembalian, status di aplikasi Anda akan otomatis berubah menjadi 'Selesai'."
            )
        }

        // 5. Denda
        binding.btnFaq5.setOnClickListener {
            navigateToDetail(
                "Apakah ada denda keterlambatan?",
                "Ya, denda keterlambatan berlaku sebesar Rp 2.000 per hari untuk setiap buku yang terlambat dikembalikan. Pastikan mengembalikan tepat waktu untuk menghindari denda."
            )
        }

        // 6. Buku Hilang
        binding.btnFaq6.setOnClickListener {
            navigateToDetail(
                "Apa yang terjadi jika buku hilang?",
                "Jika buku hilang, Anda wajib melaporkannya segera ke petugas. Anda akan diminta untuk mengganti buku yang sama atau membayar denda sesuai dengan harga pasar buku tersebut."
            )
        }
    }

    private fun navigateToDetail(title: String, answer: String) {
        val bundle = Bundle().apply {
            putString("faq_title", title)
            putString("faq_answer", answer)
        }
        findNavController().navigate(R.id.action_nav_bantuan_to_faqDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}