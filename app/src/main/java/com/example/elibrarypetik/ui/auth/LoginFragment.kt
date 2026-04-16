package com.example.elibrarypetik.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.elibrarypetik.data.api.ApiConfig
import com.example.elibrarypetik.data.api.model.LoginRequest
import com.example.elibrarypetik.data.api.model.LoginResponse
import com.example.elibrarypetik.data.pref.PreferenceManager
import com.example.elibrarypetik.databinding.FragmentLoginBinding
import com.example.elibrarypetik.ui.main.MainActivity
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Username dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Panggil API Login
            val loginRequest = LoginRequest(username, password)
            ApiConfig.getApiService().login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.status == "success") {
                            
                            // SIMPAN DATA KE PREFERENCES
                            val data = loginResponse.data
                            val prefManager = PreferenceManager(requireContext())
                            prefManager.saveUser(
                                data?.token ?: "",
                                data?.username ?: username,
                                data?.profil ?: "" // Menyimpan URL Foto
                            )

                            Toast.makeText(requireContext(), "Login Berhasil: ${loginResponse.message}", Toast.LENGTH_SHORT).show()
                            
                            // Berpindah ke MainActivity
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            Toast.makeText(requireContext(), "Login Gagal: ${loginResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error: NIM atau Password salah!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LoginFragment", "Failure: ${t.message}")
                    Toast.makeText(requireContext(), "Kesalahan Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}