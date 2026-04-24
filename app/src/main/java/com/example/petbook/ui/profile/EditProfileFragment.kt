package com.example.petbook.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.petbook.data.api.model.MahasantriUpdateResponse
import com.example.petbook.data.api.model.UpdateMahasantriRequest
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentEditProfileBinding
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            val photoSource: Any = if (!localUri.isNullOrEmpty()) Uri.parse(localUri) else serverUrl ?: R.drawable.ic_profile
            
            Glide.with(this@EditProfileFragment)
                .load(photoSource)
                .circleCrop()
                .into(ivEditProfile)
        }
    }

    private fun setupActionButtons() {
        binding.btnChangePhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSave.setOnClickListener {
            updateDataToServer()
        }
    }

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
        val mhsId = prefManager.getMahasantriId()
        val userId = prefManager.getUserId()

        if (token.isNullOrEmpty() || mhsId <= 0) {
            Toast.makeText(requireContext(), "Sesi berakhir atau ID tidak ditemukan", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Menyimpan..."

        val request = UpdateMahasantriRequest(name, jurusan, address, phone, userId)
        val authHeader = "Bearer $token"

        // FIX: Gunakan MahasantriUpdateResponse (Objek tunggal) sesuai respon API
        ApiConfig.getApiService().updateMahasantri(authHeader, mhsId, request).enqueue(object : Callback<MahasantriUpdateResponse> {
            override fun onResponse(call: Call<MahasantriUpdateResponse>, response: Response<MahasantriUpdateResponse>) {
                if (_binding != null) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Simpan Perubahan"

                    if (response.isSuccessful) {
                        prefManager.saveMahasantriDetail(mhsId, name, jurusan, address, phone)
                        Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            JSONObject(errorBody ?: "").getString("message")
                        } catch (e: Exception) {
                            "Status Code: ${response.code()}"
                        }
                        Toast.makeText(requireContext(), "Gagal: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<MahasantriUpdateResponse>, t: Throwable) {
                if (_binding != null) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Simpan Perubahan"
                    Toast.makeText(requireContext(), "Koneksi Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
