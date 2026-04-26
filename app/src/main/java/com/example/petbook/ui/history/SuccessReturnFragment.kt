package com.example.petbook.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.R
import com.example.petbook.databinding.FragmentSuccessReturnBinding

class SuccessReturnFragment : Fragment() {

    private var _binding: FragmentSuccessReturnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuccessReturnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookTitle = arguments?.getString("book_title") ?: "Buku"
        val status = arguments?.getString("status") ?: "dipinjam"

        setupUIByStatus(status, bookTitle)

        binding.btnToCatalog.setOnClickListener {
            findNavController().navigate(R.id.bookFragment)
        }

        binding.btnBackToHome.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupUIByStatus(status: String, bookTitle: String) {
        when (status.lowercase()) {
            "dipinjam" -> {
                binding.ivSuccessIcon.setImageResource(R.drawable.centang) // Pastikan icon tersedia
                binding.ivSuccessIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_blue)
                binding.tvSuccessTitle.text = "Peminjaman Berhasil!"
                binding.tvSuccessMessage.text = "Buku $bookTitle sekarang aktif dipinjam. Selamat membaca!"
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            "dikembalikan", "selesai" -> {
                binding.ivSuccessIcon.setImageResource(R.drawable.centang)
                binding.ivSuccessIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.success_green)
                binding.tvSuccessTitle.text = "Pengembalian Sukses!"
                binding.tvSuccessMessage.text = "Terima kasih telah mengembalikan buku $bookTitle tepat waktu."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
