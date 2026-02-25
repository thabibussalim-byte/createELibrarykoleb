package com.example.elibrarypetik.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.elibrarypetik.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, LoginFragment())
                .commit()
        }
    }
}