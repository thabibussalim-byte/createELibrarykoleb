package com.example.elibrarypetik.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.elibrarypetik.R
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

        // Klik tombol pinjam untuk pindah ke formulir peminjaman
        binding.btnPinjam.setOnClickListener {
            findNavController().navigate(R.id.action_detailbookFragment_to_detailpeminjamanFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}