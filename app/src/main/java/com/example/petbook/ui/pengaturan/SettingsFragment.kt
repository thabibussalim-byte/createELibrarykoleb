package com.example.petbook.ui.pengaturan

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.petbook.data.local.datastore.SettingPreferences
import com.example.petbook.data.local.datastore.ViewModelFactory
import com.example.petbook.data.local.datastore.dataStore
import com.example.petbook.databinding.FragmentSettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.core.graphics.drawable.toDrawable
import android.widget.Toast
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!


    private val prefManager: PreferenceManager by lazy {
        PreferenceManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = SettingPreferences.getInstance(requireContext().dataStore)
        val settingsViewModel =
            ViewModelProvider(this, ViewModelFactory(pref))[SettingsViewModel::class.java]


        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.saveThemeSetting(isChecked)
        }

        settingsViewModel.getThemeSettings().observe(viewLifecycleOwner) { isDarkModeActive ->
            binding.switchDarkMode.isChecked = isDarkModeActive
        }


        settingsViewModel.getNotificationSettings().observe(viewLifecycleOwner) { isNotifActive ->
            binding.switchNotif.isChecked = isNotifActive
        }

        binding.switchNotif.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.saveNotificationSetting(isChecked)
        }

        binding.btnUbahPassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }
        private fun showChangePasswordDialog() {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_change_password)
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val etOldPassword = dialog.findViewById<TextInputEditText>(R.id.et_old_password)
            val etNewPassword = dialog.findViewById<TextInputEditText>(R.id.et_new_password)
            val btnSave = dialog.findViewById<MaterialButton>(R.id.btn_save_password)
            val btnCancel = dialog.findViewById<MaterialButton>(R.id.btn_cancel_password)

            btnCancel.setOnClickListener { dialog.dismiss() }

            btnSave.setOnClickListener {
                val oldPass = etOldPassword.text.toString()
                val newPass = etNewPassword.text.toString()
                val password = prefManager.getPassword() ?: ""


                if (oldPass.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(requireContext(), "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }else if (oldPass == password){
                    updatePassword(newPass, dialog)
                }else{
                    Toast.makeText(requireContext(), "Password lama salah", Toast.LENGTH_SHORT).show()
                }


                updatePassword(newPass, dialog)
            }

            dialog.show()
        }

        private fun updatePassword(newPass: String, dialog: Dialog) {
            val token = prefManager.getToken() ?: return
            val userId = prefManager.getUserId()
            val request = UpdateUserRequest(password = newPass)

            ApiConfig.getApiService().updateUser("Bearer $token", userId, request).enqueue(object : Callback<BorrowResponse> {
                override fun onResponse(call: Call<BorrowResponse>, response: Response<BorrowResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Password lama salah", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Gagal mengubah password", Toast.LENGTH_SHORT).show()
                }
            })
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}