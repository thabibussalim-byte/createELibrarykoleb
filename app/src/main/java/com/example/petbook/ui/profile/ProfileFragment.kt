package com.example.petbook.ui.profile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
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
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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
            fetchUserData(authHeader) 
            fetchMahasantriData(authHeader)
            fetchTotalDenda(authHeader)
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val etPhoto = dialog.findViewById<TextInputEditText>(R.id.et_edit_photo)
        val etName = dialog.findViewById<TextInputEditText>(R.id.et_edit_name)
        val etPhone = dialog.findViewById<TextInputEditText>(R.id.et_edit_phone)
        val spinnerJurusan = dialog.findViewById<Spinner>(R.id.spinner_jurusan)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btn_save)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btn_cancel)

        // Set Data Awal
        etPhoto.setText(prefManager.getProfileUrl())
        etName.setText(prefManager.getMahasantriNama().ifEmpty { prefManager.getUsername() })
        etPhone.setText(prefManager.getMahasantriPhone())

        // Logic Nama: Tidak bisa diedit & muncul Toast
        etName.setOnClickListener {
            Toast.makeText(requireContext(), "Nama tidak bisa diubah", Toast.LENGTH_SHORT).show()
        }

        // Logic Jurusan (Spinner)
        val listJurusan = listOf("PSJ", "PPW", "PPM")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listJurusan)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJurusan.adapter = adapter

        val currentJurusan = prefManager.getMahasantriJurusan()
        val selection = listJurusan.indexOf(currentJurusan)
        if (selection != -1) spinnerJurusan.setSelection(selection)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newPhotoUrl = etPhoto.text.toString()
            val newJurusan = spinnerJurusan.selectedItem.toString()
            val newPhone = etPhone.text.toString()
            val fixedAlamat = "Depok, Jawa Barat, Indonesia"

            // 1. Update ke Server (Hanya Foto sesuai permintaan)
            updateProfilePhoto(newPhotoUrl)

            // 2. Simpan sisanya ke Preference Lokal
            prefManager.saveMahasantriDetail(
                prefManager.getMahasantriNama(),
                newJurusan,
                fixedAlamat,
                newPhone
            )

            dialog.dismiss()
            displayDataFromPrefs() // Refresh UI
            Toast.makeText(requireContext(), "Profil berhasil diperbarui", android.widget.Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun updateProfilePhoto(url: String) {
        val token = prefManager.getToken() ?: return
        val userId = prefManager.getUserId()
        val request = UpdateUserRequest(url)

        ApiConfig.getApiService().updateUser("Bearer $token", userId, request).enqueue(object : Callback<BorrowResponse> {
            override fun onResponse(call:Call<BorrowResponse>, response: Response<BorrowResponse>) {
                if (response.isSuccessful) {
                    prefManager.saveUser(userId, token, prefManager.getUsername() ?: "", url)
                    displayDataFromPrefs()
                }
            }
            override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {
                Log.e("Profile", "Gagal update foto: ${t.message}")
            }
        })
    }

    private fun displayDataFromPrefs() {
        binding.apply {
            val nama = prefManager.getMahasantriNama()
            tvProfileFullName.text = nama.ifEmpty { prefManager.getUsername() }
            
            tvProfileJurusan.text = prefManager.getMahasantriJurusan().ifEmpty { "-" }
            tvProfileAlamat.text = prefManager.getMahasantriAlamat().ifEmpty { "-" }
            tvProfilePhone.text = prefManager.getMahasantriPhone().ifEmpty { "-" }

            val photoUrl = prefManager.getProfileUrl()
            Glide.with(this@ProfileFragment)
                .load(photoUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(ivProfilePicture)
        }
    }

    private fun fetchUserData(authHeader: String) {
        val currentUserId = prefManager.getUserId()
        showLoading(true)
        
        ApiConfig.getApiService().getUsers(authHeader).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (_binding != null) {
                    showLoading(false)
                    if (response.isSuccessful) {
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
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                if (_binding != null) showLoading(false)
                Log.e("Profile", "Gagal fetch User: ${t.message}")
            }
        })
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
        
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val allTransactions = response.body()?.data ?: emptyList()
                    val userTransactions = allTransactions.filter { it.userId == userId }
                    val transactionIds = userTransactions.map { it.id }
                    
                    ApiConfig.getApiService().getFines(authHeader).enqueue(object : Callback<FineResponse> {
                        override fun onResponse(call: Call<FineResponse>, response: Response<FineResponse>) {
                            if (_binding != null && response.isSuccessful) {
                                val allFines = response.body()?.data ?: emptyList()
                                val total = allFines
                                    .filter { it.transaksiId in transactionIds && it.status == "belumdibayar" }
                                    .sumOf { it.totalDenda.toIntOrNull() ?: 0 }
                                
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

        binding.cardStatistik.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_statistikFragment)
        }

        binding.btnViewPhoto.setOnClickListener {
            showExpandedPhoto()
        }
    }

    private fun showExpandedPhoto() {
        val photoUrl = prefManager.getProfileUrl()
        if (photoUrl.isNullOrEmpty()) return

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_full_image)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val imageView = dialog.findViewById<ImageView>(R.id.iv_full_image)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_dialog)

        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(imageView)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
