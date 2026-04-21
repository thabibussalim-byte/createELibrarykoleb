package com.example.petbook.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    // Launcher untuk memilih foto dari galeri
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Simpan URI secara lokal di Preference
            prefManager.saveLocalProfileUri(uri.toString())
            // Langsung update tampilan
            displayDataFromPrefs()
        }
    }

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
            fetchUserData(authHeader)
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

            // LOGIKA PHOTO: Cek lokal dulu, kalau tidak ada baru dari server
            val localUri = prefManager.getLocalProfileUri()
            val serverUrl = prefManager.getProfileUrl()
            
            val photoSource = if (!localUri.isNullOrEmpty()) {
                Uri.parse(localUri)
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
                    if (myAccount != null && !myAccount.profil.isNullOrEmpty()) {
                        prefManager.saveUser(
                            currentUserId, 
                            prefManager.getToken() ?: "", 
                            prefManager.getUsername() ?: "", 
                            myAccount.profil
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
        val currentUsername = prefManager.getUsername()?.lowercase() ?: ""
        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    val myData = list.find { it.userId == currentUserId || it.namaMahasantri.lowercase().contains(currentUsername) }
                    if (myData != null) {
                        prefManager.saveMahasantriDetail(myData.namaMahasantri, myData.jurusan, myData.alamat, myData.noHp)
                        displayDataFromPrefs()
                    }
                }
            }
            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {}
        })
    }

    private fun fetchTotalDenda(authHeader: String) {
        val userId = prefManager.getUserId()
        ApiConfig.getApiService().getHistoryByUser(authHeader, userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    if (response.isSuccessful) {
                        val transactions = response.body()?.data ?: emptyList()
                        calculateDendaFromTransactions(authHeader, transactions)
                    } else {
                        fallbackFetchAllHistory(authHeader, userId)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                fallbackFetchAllHistory(authHeader, userId)
            }
        })
    }

    private fun fallbackFetchAllHistory(authHeader: String, userId: Int) {
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
        // Klik tombol kamera untuk ganti foto
        binding.btnEditPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.cardDenda.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_detailDendaFragment)
        }
        binding.cardStatistik.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_statistikFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
