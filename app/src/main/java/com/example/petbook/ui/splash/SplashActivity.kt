package com.example.petbook.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.petbook.R
import com.example.petbook.ui.auth.AuthActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen API sebelum super.onCreate
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 2. Beri waktu splash screen sistem muncul, lalu lanjut ke AuthActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
