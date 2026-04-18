package com.example.petbook.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.MahasantriResponse
import com.example.petbook.data.api.model.FineResponse
import com.example.petbook.data.api.model.HistoryResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())

        displayDataFromPrefs()
        setupClickListeners()
        
        val token = prefManager.getToken()
        if (!token.isNullOrEmpty()) {
            val authHeader = "Bearer $token"
            fetchMahasantriData(authHeader)
            fetchTotalDenda(authHeader)
        }
    }

    private fun displayDataFromPrefs() {
        binding.apply {
            val nama = prefManager.getMahasantriNama()
            tvProfileFullName.text = if (nama.isNotEmpty()) nama else prefManager.getUsername()
            
            tvProfileJurusan.text = prefManager.getMahasantriJurusan().ifEmpty { "-" }
            tvProfileAlamat.text = prefManager.getMahasantriAlamat().ifEmpty { "-" }
            tvProfilePhone.text = prefManager.getMahasantriPhone().ifEmpty { "-" }

            Glide.with(this@ProfileFragment)
                .load(prefManager.getProfileUrl())
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(ivProfilePicture)
        }
    }

    private fun fetchMahasantriData(authHeader: String) {
        val currentUserId = prefManager.getUserId()
        val currentUsername = prefManager.getUsername()?.lowercase() ?: ""

        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    val myData = list.find { 
                        it.userId == currentUserId || 
                        it.namaMahasantri.lowercase().contains(currentUsername)
                    }
                    
                    if (myData != null) {
                        prefManager.saveMahasantriDetail(
                            myData.namaMahasantri,
                            myData.jurusan,
                            myData.alamat,
                            myData.noHp
                        )
                        displayDataFromPrefs()
                    }
                }
            }
            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {
                Log.e("Profile", "Gagal fetch Mahasantri: ${t.message}")
            }
        })
    }

    private fun fetchTotalDenda(authHeader: String) {
        val userId = prefManager.getUserId()
        
        // Step 1: Ambil transaksi user untuk mendapatkan ID Transaksi miliknya
        ApiConfig.getApiService().getHistoryByUser(authHeader, userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val userTransactions = response.body()?.data ?: emptyList()
                    val transactionIds = userTransactions.map { it.id }
                    
                    // Step 2: Ambil semua denda dan filter yang milik user
                    ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
                        override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                            if (_binding != null && response.isSuccessful) {
                                val allFines = response.body()?.data ?: emptyList()
                                val userFines = allFines.filter { it.transaksiId in transactionIds }
                                
                                var total = 0
                                userFines.forEach { total += it.totalDenda.toIntOrNull() ?: 0 }
                                binding.tvTotalDenda.text = "Rp $total"
                            }
                        }
                        override fun onFailure(call: Call<FineResponse>, t: Throwable) {}
                    })
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {}
        })
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