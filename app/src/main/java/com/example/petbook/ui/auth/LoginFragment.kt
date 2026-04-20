package com.example.petbook.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.LoginRequest
import com.example.petbook.data.api.model.LoginResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.data.session.SessionManager
import com.example.petbook.databinding.FragmentLoginBinding
import com.example.petbook.ui.main.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    // 1. Tambahkan fungsi untuk kontrol loading (pastikan ada ProgressBar di XML Anda)
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefManager = PreferenceManager(requireContext())

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Data tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tampilkan loading segera agar aplikasi terasa responsif
            showLoading(true)

            val loginRequest = LoginRequest(username, password)
            ApiConfig.getApiService().login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    // Sembunyikan loading segera setelah dapat respon
                    showLoading(false)

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.status == "success") {
                            val data = loginResponse.data

                            // Simpan data
                            prefManager.saveLogin(
                                data?.token ?: "",
                                data?.username ?: username
                            )

                            val sessionManager = SessionManager(requireContext())
                            sessionManager.saveLoginSession(data?.token ?: "")


                            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                                // Flag ini membuat transisi lebih bersih
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            Toast.makeText(requireContext(), loginResponse?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Username atau Password salah!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e("LoginFragment", "Failure: ${t.message}")
                    Toast.makeText(requireContext(), "Masalah Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}