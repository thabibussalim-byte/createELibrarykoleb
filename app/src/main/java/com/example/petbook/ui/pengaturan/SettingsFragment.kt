package com.example.petbook.ui.pengaturan

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.local.datastore.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentSettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val prefManager by lazy { PreferenceManager(requireContext()) }

    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext(), SettingPreferences.getInstance(requireContext().dataStore))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsViewModel.getThemeSettings().observe(viewLifecycleOwner) { isDarkMode ->
            binding.switchDarkMode.isChecked = isDarkMode
            AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.saveThemeSetting(isChecked)
        }

        settingsViewModel.getNotificationSettings().observe(viewLifecycleOwner) { isActive ->
            binding.switchNotif.isChecked = isActive
        }
        binding.switchNotif.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.saveNotificationSetting(isChecked)
        }


        binding.btnUbahPassword.setOnClickListener { showChangePasswordDialog() }
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_change_password)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val btnSave = dialog.findViewById<MaterialButton>(R.id.btn_save_password)
        val btnCencel = dialog.findViewById<MaterialButton>(R.id.btn_cancel_password)
        val etOld = dialog.findViewById<TextInputEditText>(R.id.et_old_password)
        val etNew = dialog.findViewById<TextInputEditText>(R.id.et_new_password)

        btnCencel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val oldPass = etOld.text.toString()
            val newPass = etNew.text.toString()
            if (oldPass == prefManager.getPassword()) {
                updatePassword(newPass, dialog)
            } else {
                Toast.makeText(context, "Password lama salah", Toast.LENGTH_SHORT).show()
            }
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
                    prefManager.saveUser(userId, token, newPass, prefManager.getUsername() ?: "", prefManager.getProfileUrl() ?: "")
                    dialog.dismiss()
                    Toast.makeText(context, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {}
        })
    }
}