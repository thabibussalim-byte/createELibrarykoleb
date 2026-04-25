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
        
        // Kita gunakan API getMahasantri sebagai tes "apakah pintu terbuka?"
        ApiConfig.getApiService().getMahasantri(authHeader).enqueue(object : Callback<MahasantriResponse> {
            override fun onResponse(call: Call<MahasantriResponse>, response: Response<MahasantriResponse>) {
                if (response.isSuccessful) {
                    // Token VALID -> Lanjut ke Home
                    navigateToHome()
                } else {
                    // Token KADALUARSA atau GA VALID -> Logout dan Login ulang
                    prefManager.clear() // Hapus sampah token lama
                    Toast.makeText(this@SplashActivity, "Sesi berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }

            override fun onFailure(call: Call<MahasantriResponse>, t: Throwable) {
                // Masalah Koneksi -> Tetap izinkan masuk (Offline mode sementara) atau paksa login
                // Di sini kita izinkan masuk saja agar user tidak terjebak jika sinyal jelek
                navigateToHome()
            }
        })
    }

    private fun navigateToLogin() {
        //berpindah halaman ke login untuk melakukan login ulang agar mendapatkan token baru yang valid
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToHome() {
        //berpindah halaman ke daasboard
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
