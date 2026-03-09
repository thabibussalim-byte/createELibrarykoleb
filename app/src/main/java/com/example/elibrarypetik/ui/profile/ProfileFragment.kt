package com.example.elibrarypetik.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProfileData()
        setupClickListeners()
    }

    private fun setupProfileData() {
        binding.tvProfileUsername.text = "aril_stiven"
        binding.tvProfileDesc.text = "Pencinta kopi & petualangan imajinasi lewat buku."
        
        val photoUrl: String? = null
        
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)

        val totalDenda = 2000
        binding.tvTotalDenda.text = "Rp $totalDenda"
        
        if (totalDenda > 0) {
            binding.tvTotalDenda.setTextColor(resources.getColor(R.color.error_red, null))
        } else {
            binding.tvTotalDenda.text = "Tidak ada denda"
            binding.tvTotalDenda.setTextColor(resources.getColor(R.color.success_green, null))
        }
    }

    private fun setupClickListeners() {
        binding.btnEditPhoto.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Galeri...", Toast.LENGTH_SHORT).show()
        }

        binding.cardDenda.setOnClickListener {
            // PASTIKAN ID INI SAMA PERSIS DENGAN DI nav_main.xml
            try {
                findNavController().navigate(R.id.action_profileFragment_to_detailDendaFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigasi Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.cardStatistik.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka statistik...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}