package com.example.petbook.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.petbook.R
import com.example.petbook.data.local.room.AppDatabase
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.ui.main.MainActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefManager = PreferenceManager(this)

        // Cek Sesi: Jika sudah login, langsung ke MainActivity
        if (prefManager.isLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        setContentView(R.layout.activity_auth)
    }


    fun logout() {
        lifecycleScope.launch {
            // 1. Hapus Cache Database Room
            val database = AppDatabase.getInstance(this@AuthActivity)
            database.clearAllTables()

            // 2. Hapus Sesi SharedPreferences
            prefManager.clear()

            // 3. Restart Activity untuk menampilkan Login
            val intent = Intent(this@AuthActivity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}