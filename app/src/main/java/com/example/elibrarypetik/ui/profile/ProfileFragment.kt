package com.example.elibrarypetik.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.pref.PreferenceManager
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

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        val prefManager = PreferenceManager(requireContext())
        
        // Menampilkan Nama dari Preferences
        binding.tvProfileUsername.text = prefManager.getUsername()
        binding.tvProfileDesc.text = "Santri PeTIK - Pencinta ilmu & teknologi."
        
        // Menampilkan Foto dari URL API menggunakan Glide
        Glide.with(this)
            .load(prefManager.getProfileUrl())
            .placeholder(R.drawable.ic_profile)
            .circleCrop()
            .into(binding.ivProfilePicture)
            
        binding.tvTotalDenda.text = "Rp 0" // Default
    }

    private fun setupClickListeners() {
        binding.cardDenda.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_detailDendaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}