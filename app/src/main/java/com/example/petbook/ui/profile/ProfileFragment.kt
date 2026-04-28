package com.example.petbook.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import androidx.core.net.toUri

@Suppress("DEPRECATION")
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
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            fetchUserData(authHeader)
            fetchMahasantriData(authHeader)
            fetchTotalDenda(authHeader)
        }
    }

    private fun displayDataFromPrefs() {
        binding.apply {
            val nama = prefManager.getMahasantriNama()
            tvProfileFullName.text = nama.ifEmpty { prefManager.getUsername() ?: "-" }
            
            tvProfileJurusan.text = prefManager.getMahasantriJurusan().ifEmpty { "-" }
            tvProfileAlamat.text = prefManager.getMahasantriAlamat().ifEmpty { "-" }
            tvProfilePhone.text = prefManager.getMahasantriPhone().ifEmpty { "-" }

            val localUri = prefManager.getLocalProfileUri()
            val serverUrl = prefManager.getProfileUrl()
            
            val photoSource = if (!localUri.isNullOrEmpty()) {
                localUri.toUri()
            } else {
                serverUrl
            }

            Glide.with(this@ProfileFragment)
                .load(photoSource)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(ivProfilePicture)
        }
    }

    private fun fetchUserData(authHeader: String) {
        val currentUserId = prefManager.getUserId()
        ApiConfig.getApiService().getUsers(authHeader).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val userList = response.body()?.data ?: emptyList()
                    val myAccount = userList.find { it.id == currentUserId }
                    if (myAccount != null) {
                        prefManager.saveUser(
                            myAccount.id,
                            prefManager.getToken() ?: "",
                            myAccount.username,
                            prefManager.getPassword() ?: "",
                            myAccount.profil ?: ""
                        )
                        displayDataFromPrefs()
                    }
                }
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {}
        })
    }

    private fun fetchMahasantriData(authHeader: String) {
        val currentUserId = prefManager.getUserId()
        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    val myData = list.find { it.userId == currentUserId }
                    if (myData != null) {
                        prefManager.saveMahasantriDetail(myData.id, myData.namaMahasantri, myData.jurusan, myData.alamat, myData.noHp)
                        displayDataFromPrefs()
                    }
                }
            }
            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {}
        })
    }

    private fun fetchTotalDenda(authHeader: String) {
        val userId = prefManager.getUserId()
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val allTransactions = response.body()?.data ?: emptyList()
                    val userTransactions = allTransactions.filter { it.userId == userId }
                    calculateDendaFromTransactions(authHeader, userTransactions)
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {}
        })
    }

    private fun calculateDendaFromTransactions(authHeader: String, transactions: List<HistoryDataItem>) {
        val transactionIds = transactions.map { it.id }
        ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
            override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val allFines = response.body()?.data ?: emptyList()
                    val unpaidFines = allFines.filter { 
                        it.transaksiId in transactionIds && it.status.lowercase() != "dibayar" 
                    }
                    var total = 0
                    unpaidFines.forEach { fine ->
                        val cleanAmount = fine.totalDenda.replace(Regex("[^0-9]"), "")
                        total += cleanAmount.toIntOrNull() ?: 0
                    }
                    if (_binding != null) {
                        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        binding.tvTotalDenda.text = formatRupiah.format(total).replace(",00", "").replace("Rp", "Rp ")
                    }
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            }
        }

        binding.cardDenda.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                findNavController().navigate(R.id.action_profileFragment_to_detailDendaFragment)
            }
        }

        binding.cardStatistik.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.profileFragment) {
                findNavController().navigate(R.id.action_profileFragment_to_statistikFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
