package com.example.elibrarypetik.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
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

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, 
                R.id.bookFragment, 
                R.id.historyFragment, 
                R.id.profileFragment
            ),
            binding.drawerLayout
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        setSupportActionBar(binding.toolbar)

        binding.bottomNav.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        // Panggil fungsi untuk memuat foto profil di Drawer Header
        updateDrawerHeader()

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

    /**
     * Fungsi untuk memperbarui Foto Profil di Drawer Header secara dinamis
     */
    fun updateDrawerHeader() {
        val headerView: View = binding.navigationView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.iv_profile_header)

        // Contoh: Ambil URL foto dari SharedPreferences atau API
        // Untuk sekarang kita gunakan link dummy, nanti bisa diganti data asli
        val latestPhotoUrl = "https://picsum.photos/id/64/200/200" 

        Glide.with(this)
            .load(latestPhotoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .into(ivProfileHeader)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}