package com.example.petbook.ui.profile

import android.net.Uri
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
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        displayDataFromPrefs()
        loadApiData()
    }

    private fun loadApiData() {
        val token = prefManager.getToken()
        if (!token.isNullOrEmpty()) {
            // Tampilkan Loading Premium
            binding.progressBarProfile.visibility = View.VISIBLE
            
            val authHeader = "Bearer $token"
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
            val photoSource: Any = if (!localUri.isNullOrEmpty()) Uri.parse(localUri) else serverUrl ?: R.drawable.ic_profile

            Glide.with(this@ProfileFragment)
                .load(photoSource)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(ivProfilePicture)
        }
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

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        binding.cardDenda.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_detailDendaFragment)
        }
        binding.cardStatistik.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_statistikFragment)
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
                        prefManager.saveUser(myAccount.id, prefManager.getToken() ?: "", myAccount.username, prefManager.getPassword() ?: "", myAccount.profil ?: "")
                        displayDataFromPrefs()
                    }
                }
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {}
        })
    }

    private fun fetchTotalDenda(authHeader: String) {
        val userId = prefManager.getUserId()
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val rawData = response.body()?.data ?: emptyList()
                    val myTransactions = rawData.filter { it.userId == userId }
                    processFines(authHeader, myTransactions)
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarProfile.visibility = View.GONE
            }
        })
    }

    private fun processFines(authHeader: String, transactions: List<HistoryDataItem>) {
        val myTransactionIds = transactions.map { it.id }.toSet()
        ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
            override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                if (_binding != null) {
                    // Sembunyikan Loading
                    binding.progressBarProfile.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        val allFines = response.body()?.data ?: emptyList()
                        
                        // JUMLAHKAN SEMUA: Karena data duplikat sudah dihapus di server, total akan pas Rp 16.000
                        val myUnpaidFines = allFines.filter { 
                            it.transaksiId in myTransactionIds && it.status.lowercase() != "dibayar" 
                        }
                        
                        var totalDenda = 0
                        myUnpaidFines.forEach { fine ->
                            val amountStr = fine.totalDenda.filter { it.isDigit() }
                            totalDenda += amountStr.toIntOrNull() ?: 0
                        }
                        
                        try {
                            val localeID = Locale("id", "ID")
                            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
                            binding.tvTotalDenda.text = formatRupiah.format(totalDenda).replace("Rp", "Rp ").replace(",00", "")
                        } catch (e: Exception) {
                            binding.tvTotalDenda.text = "Rp $totalDenda"
                        }
                    }
                }
            }
            override fun onFailure(call: Call<FineResponse>, t: Throwable) {
                if (_binding != null) binding.progressBarProfile.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
