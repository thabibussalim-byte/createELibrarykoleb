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
                "Prosedur dan Tata Cara Peminjaman Koleksi",
                "Untuk memulai peminjaman, silakan jelajahi koleksi melalui menu Katalog Digital. Setelah menemukan buku yang diinginkan, klik tombol 'Ajukan Peminjaman'. Sistem akan mengirimkan formulir digital kepada admin perpustakaan. Mohon untuk tidak datang ke perpustakaan PeTIK sebelum Anda menerima notifikasi 'Disetujui' di aplikasi. Setelah disetujui, Anda memiliki waktu 1x24 jam untuk melakukan serah terima fisik di meja sirkulasi."
            )
        }

        // 2. Waktu Peminjaman
        binding.btnFaq2.setOnClickListener {
            navigateToDetail(
                "Ketentuan Durasi dan Batas Waktu Pinjam",
                "Masa peminjaman koleksi di ElibraryPetik diatur selama 7 hari kalender terhitung sejak buku fisik diserahkan oleh petugas. Kami sangat menyarankan Anda untuk memantau menu Riwayat Peminjaman secara berkala guna melihat detail tanggal jatuh tempo. Sistem juga akan mengirimkan peringatan otomatis melalui notifikasi aplikasi tepat 24 jam sebelum masa pinjam Anda berakhir untuk menghindari keterlambatan."
            )
        }

        // 3. Status Buku
        binding.btnFaq3.setOnClickListener {
            navigateToDetail(
                "Penjelasan Detail Mengenai Status Peminjaman",
                "• Menunggu Verifikasi: Permintaan Anda telah masuk ke sistem dan sedang menunggu antrean pengecekan fisik buku oleh petugas.\n\n• Disetujui: Buku tersedia dan telah disisihkan untuk Anda. Silakan ambil di loket perpustakaan atau kantor PeTIK.\n\n• Dipinjam: Buku telah berada dalam tanggung jawab Anda sepenuhnya.\n\n• Ditolak: Permintaan dibatalkan sistem, biasanya karena stok buku habis, kondisi buku sedang diperbaiki, atau Anda masih memiliki pinjaman tertunggak.",
            )

        }

        // 4. Cara Mengembalikan
        binding.btnFaq4.setOnClickListener {
            navigateToDetail(
                "Prosedur Resmi Pengembalian Koleksi",
                "Pengembalian wajib dilakukan secara langsung kepada petugas sirkulasi untuk pengecekan fisik. Jangan meninggalkan buku di meja tanpa verifikasi petugas. Setelah petugas memeriksa buku, pastikan Anda memeriksa status di aplikasi telah berubah menjadi 'Selesai'. Hal ini penting untuk memastikan bahwa tanggung jawab peminjaman Anda telah terhapus dari sistem dan tidak memicu denda di kemudian hari.",
            )

        }

        // 5. Denda
        binding.btnFaq5.setOnClickListener {
            navigateToDetail(
                "Regulasi dan Akumulasi Denda Keterlambatan",
                "Berdasarkan peraturan Pesantren PeTIK, setiap keterlambatan pengembalian akan dikenakan denda administratif sebesar Rp 1.000 per hari untuk tiap judul buku. Akumulasi denda yang belum dilunasi akan menyebabkan akun Anda terkunci secara otomatis oleh sistem, sehingga Anda tidak dapat melakukan peminjaman buku baru sampai seluruh kewajiban administrasi diselesaikan di bagian keuangan perpustakaan PeTIK.",
            )

        }

        // 6. Buku Hilang
        binding.btnFaq6.setOnClickListener {
            navigateToDetail(
                "Kebijakan Kehilangan dan Kerusakan Koleksi",
                "Koleksi perpustakaan adalah aset bersama. Jika terjadi kerusakan (sobek, terkena air, coretan) atau kehilangan, peminjam wajib melapor segera kepada petugas. Sesuai ketentuan, Anda diwajibkan mengganti dengan buku yang sama (judul, penulis, dan penerbit yang identik) atau membayar biaya penggantian sebesar harga pasar buku saat ini ditambah biaya administrasi pengadaan sebesar 10% dari harga buku.",
            )

        }

        // 7. Perpanjang Masa Pinjam
        binding.btnFaq7.setOnClickListener {
            navigateToDetail(
                "Ketentuan Perpanjangan Masa Peminjaman",
                "Perpanjangan masa pinjam dapat dilakukan untuk durasi 7 hari tambahan dan hanya dapat diajukan sebanyak 1 kali. Fitur perpanjangan ini akan muncul di aplikasi 2 hari sebelum jatuh tempo, dengan catatan buku tersebut tidak sedang masuk dalam daftar reservasi atau antrean santri lain. Jika fitur perpanjangan tidak muncul, berarti buku tersebut harus segera dikembalikan sesuai jadwal aslinya.",
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