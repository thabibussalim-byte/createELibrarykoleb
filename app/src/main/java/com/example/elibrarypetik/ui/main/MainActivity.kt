package com.example.elibrarypetik.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.ActivityMainBinding
import com.example.elibrarypetik.ui.auth.AuthActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inisialisasi NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 2. Konfigurasi AppBar (Top Level Destinations + Drawer Layout)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, 
                R.id.bookFragment, 
                R.id.historyFragment, 
                R.id.profileFragment
            ),
            binding.drawerLayout
        )

        // 3. Setup Toolbar LANGSUNG dengan NavController
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        
        setSupportActionBar(binding.toolbar)

        // 4. Hubungkan Bottom Navigation & Drawer Navigation dengan NavController
        binding.bottomNav.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        // 5. Logika khusus untuk Logout
        binding.navigationView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_logout) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(item, navController)
                if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
                handled
            }
        }

        // 6. Penanganan tombol back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (!navController.navigateUp()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        // PERBAIKAN: Gunakan NavigationUI.navigateUp agar parameter appBarConfiguration diterima dengan benar
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}