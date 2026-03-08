package com.example.elibrarypetik.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.FragmentDetailpeminjamanBinding

class DetailpeminjamanFragment : Fragment() {

    private var _binding: FragmentDetailpeminjamanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailpeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Logika Tombol Konfirmasi Peminjaman
        binding.btnPinjamFinal.setOnClickListener {
            val tglPinjam = binding.etTglPinjam.text.toString()
            val lamaPinjam = binding.etLamaPinjam.text.toString()

            if (lamaPinjam.isNotEmpty()) {
                // Simulasi Berhasil
                Toast.makeText(requireContext(), "Berhasil Mengajukan Peminjaman", Toast.LENGTH_SHORT).show()
                
                // Alur Nomor 1: Langsung pindah ke halaman Status Peminjaman
                findNavController().navigate(R.id.action_detailpeminjamanFragment_to_statuspeminjamanFragment)
            } else {
                Toast.makeText(requireContext(), "Harap isi lama peminjaman", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}