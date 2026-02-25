package com.example.elibrarypetik.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.ActivityMainBinding
import com.example.elibrarypetik.ui.auth.AuthActivity
import com.example.elibrarypetik.ui.auth.tentang.AboutFragment
import com.example.elibrarypetik.ui.bantuan.HelpFragment
import com.example.elibrarypetik.ui.buku.BookFragment
import com.example.elibrarypetik.ui.history.HistoryFragment
import com.example.elibrarypetik.ui.home.HomeFragment
import com.example.elibrarypetik.ui.pengaturan.SettingsFragment
import com.example.elibrarypetik.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toogle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SET TOOLBAR
        setSupportActionBar(binding.toolbar)

        // TOMBOL HUMBERGER
        toogle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )

        binding.drawerLayout.addDrawerListener(toogle)
        toogle.syncState()

        // SET HOME DEFAULT
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->

            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_status -> HistoryFragment()
                R.id.nav_buku -> BookFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pengaturan -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .commit()
                }

                R.id.nav_bantuan -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HelpFragment())
                        .commit()
                }

                R.id.nav_tentang -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AboutFragment())
                        .commit()
                }

                R.id.nav_logout -> {
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true

        }
    }
}