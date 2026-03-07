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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 1. Setup Toolbar
        setSupportActionBar(binding.toolbar)
        
        // 2. Konfigurasi AppBar (Top Level Destinations + Drawer Layout)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, 
                R.id.bookFragment, 
                R.id.historyFragment, 
                R.id.profileFragment
            ),
            binding.drawerLayout
        )
        
        // 3. Sinkronisasi Toolbar dengan NavController dan Drawer
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)

        // 4. Setup Bottom Navigation
        binding.bottomNav.setupWithNavController(navController)

        // 5. Setup Drawer (NavigationView)
        binding.navigationView.setupWithNavController(navController)

        // Manual click listener for Drawer to ensure it opens
        binding.toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                // Check if we are at a top-level destination to show hamburger, otherwise navigate up
                val topLevelDestinations = setOf(
                    R.id.homeFragment, R.id.bookFragment, 
                    R.id.historyFragment, R.id.profileFragment
                )
                if (navController.currentDestination?.id in topLevelDestinations) {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    navController.navigateUp()
                }
            }
        }

        // Logout logic
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

        // Back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, binding.drawerLayout) || super.onSupportNavigateUp()
    }
}