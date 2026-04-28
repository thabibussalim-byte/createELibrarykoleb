package com.example.petbook.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.MahasantriResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.ui.auth.AuthActivity
import com.example.petbook.ui.main.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefManager = PreferenceManager(this)
        val token = prefManager.getToken()

        if (token.isNullOrEmpty()) {
            navigateToLogin()
        } else {
            validateToken(token, prefManager)
        }
    }

    private fun validateToken(token: String, prefManager: PreferenceManager) {
        val authHeader = "Bearer $token"
        
        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (response.isSuccessful) {
                    navigateToHome()
                } else {
                    prefManager.clear()
                    Toast.makeText(this@SplashActivity, "Sesi berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }

            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {
                navigateToHome()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
