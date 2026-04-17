package com.example.petbook.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // 1. Tombol Utama: Pinjam Buku Lagi (Arahkan ke Katalog)
        binding.btnToCatalog.setOnClickListener {
            findNavController().navigate(R.id.bookFragment)
        }

        // 2. Tombol Sekunder: Kembali ke Beranda
        binding.btnBackToHome.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}