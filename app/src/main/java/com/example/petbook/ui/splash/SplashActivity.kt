package com.example.petbook.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.petbook.R
import com.example.petbook.ui.auth.AuthActivity

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Coba gunakan setContentView secara langsung tanpa ViewBinding sementara
        try {
            setContentView(R.layout.activity_splash)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 3000)
    }
}