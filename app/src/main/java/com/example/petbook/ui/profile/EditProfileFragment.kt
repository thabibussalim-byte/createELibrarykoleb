package com.example.petbook.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.MahasantriResponse
import com.example.petbook.data.api.model.MahasantriUpdateResponse
import com.example.petbook.data.api.model.UpdateMahasantriRequest
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentEditProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.net.toUri

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val contentResolver = requireContext().contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) { e.printStackTrace() }

            prefManager.saveLocalProfileUri(uri.toString())
            loadCurrentData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupSpinner()
        loadCurrentData()
        setupActionButtons()

        if (prefManager.getMahasantriId() <= 0) {
            fetchMahasantriData()
        }
    }

    private fun setupSpinner() {
        val jurusanList = arrayOf("PPW", "PSJ", "PPM")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, jurusanList)
        binding.spinnerJurusan.adapter = adapter
    }

    private fun loadCurrentData() {
        binding.apply {
            etEditName.setText(prefManager.getMahasantriNama())
            etEditAddress.setText(prefManager.getMahasantriAlamat())
            etEditPhone.setText(prefManager.getMahasantriPhone())

            val currentJurusan = prefManager.getMahasantriJurusan()
            val adapter = spinnerJurusan.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(currentJurusan)
            if (position >= 0) spinnerJurusan.setSelection(position)

            val localUri = prefManager.getLocalProfileUri()
            val serverUrl = prefManager.getProfileUrl()
            val photoSource: Any = if (!localUri.isNullOrEmpty()) localUri.toUri() else serverUrl ?: R.drawable.ic_profile
            
            Glide.with(this@EditProfileFragment)
                .load(photoSource)
                .circleCrop()
                .into(ivEditProfile)
        }
    }

    private fun fetchMahasantriData() {
        val token = prefManager.getToken() ?: return
        val authHeader = "Bearer $token"
        val currentUserId = prefManager.getUserId()
        val currentUsername = prefManager.getUsername()?.lowercase() ?: ""
        
        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    val myData = list.find { it.userId == currentUserId } 
                                ?: list.find { it.namaMahasantri.lowercase() == currentUsername }
                    
                    if (myData != null) {
                        prefManager.saveMahasantriDetail(
                            myData.id, 
                            myData.namaMahasantri, 
                            myData.jurusan, 
                            myData.alamat, 
                            myData.noHp
                        )
                        loadCurrentData()
                    }
                }
            }
            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {}
        })
    }

    private fun setupActionButtons() {
        binding.btnChangePhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSave.setOnClickListener {
            updateDataToServer()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDataToServer() {
        val name = binding.etEditName.text.toString().trim()
        val jurusan = binding.spinnerJurusan.selectedItem.toString()
        val address = binding.etEditAddress.text.toString().trim()
        val phone = binding.etEditPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etEditName.error = "Nama tidak boleh kosong"
            return
        }

        val token = prefManager.getToken()
        val userId = prefManager.getUserId()
        val username = prefManager.getUsername()?.lowercase() ?: ""
        var mhsId = prefManager.getMahasantriId()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Sesi berakhir, silakan login ulang", Toast.LENGTH_LONG).show()
            return
        }

        val authHeader = "Bearer $token"

        if (mhsId <= 0) {
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Mencari ID..."

            ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
                override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                    if (response.isSuccessful) {
                        val list = response.body()?.data ?: emptyList()
                        val myData = list.find { it.userId == userId } 
                                    ?: list.find { it.namaMahasantri.lowercase() == username }

                        if (myData != null) {
                            prefManager.saveMahasantriDetail(myData.id, myData.namaMahasantri, myData.jurusan, myData.alamat, myData.noHp)
                            executeActualUpdate(authHeader, myData.id, name, jurusan, address, phone, userId)
                        } else {
                            resetSaveButton()
                            Log.e("EditProfile", "Gagal temukan data. UserID: $userId, ListSize: ${list.size}")
                            Toast.makeText(requireContext(), "Akun Anda belum terdaftar sebagai Mahasantri di server.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        resetSaveButton()
                        Toast.makeText(requireContext(), "Gagal terhubung ke server (Error ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {
                    resetSaveButton()
                    Toast.makeText(requireContext(), "Masalah koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            executeActualUpdate(authHeader, mhsId, name, jurusan, address, phone, userId)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun executeActualUpdate(
        authHeader: String,
        mhsId: Int,
        name: String,
        jurusan: String,
        address: String,
        phone: String,
        userId: Int
    ) {
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Menyimpan..."

        val request = UpdateMahasantriRequest(name, jurusan, address, phone, userId)

        ApiConfig.getApiService().updateMahasantri(authHeader, mhsId, request).enqueue(object : Callback<MahasantriUpdateResponse> {
            override fun onResponse(call: Call<MahasantriUpdateResponse>, response: Response<MahasantriUpdateResponse>) {
                if (_binding != null) {
                    resetSaveButton()
                    if (response.isSuccessful) {
                        prefManager.saveMahasantriDetail(mhsId, name, jurusan, address, phone)
                        Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("EditProfile", "Update Gagal: $errorBody")
                        Toast.makeText(requireContext(), "Gagal memperbarui data di server", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<MahasantriUpdateResponse>, t: Throwable) {
                if (_binding != null) {
                    resetSaveButton()
                    Toast.makeText(requireContext(), "Koneksi Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun resetSaveButton() {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "Simpan Perubahan"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
