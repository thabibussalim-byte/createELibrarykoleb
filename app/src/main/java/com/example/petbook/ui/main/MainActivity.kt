package com.example.petbook.ui.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.LogoutResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.ActivityMainBinding
import com.example.petbook.ui.auth.AuthActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Buat Activity Full Screen (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Tentukan fragment utama (Top Level)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.bookFragment, R.id.historyFragment, R.id.profileFragment),
            binding.drawerLayout
        )

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Hubungkan BottomNav standar dengan NavController
        binding.bottomNav.setupWithNavController(navController)
        
        setupDrawer()
        handleWindowInsets() // Tambahkan fungsi penangan insets agar tidak terpotong

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Pastikan Status Bar selalu berwarna Biru Cerah (Accent Blue)
            window.statusBarColor = ContextCompat.getColor(this, R.color.accent_blue)
            
            // Matikan title centered agar teks selalu di kiri
            binding.toolbar.isTitleCentered = false
            binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            
            if (destination.id in appBarConfiguration.topLevelDestinations) {
                supportActionBar?.title = "PeTbook"
                binding.toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.white))
                binding.bottomNav.visibility = View.VISIBLE
            } else {
                supportActionBar?.title = destination.label
                binding.toolbar.setNavigationIcon(R.drawable.ic_back_ios)
                binding.toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.white))
                binding.toolbar.contentInsetStartWithNavigation = 0
                binding.bottomNav.visibility = View.GONE
            }
        }
        
        updateDrawerHeader()
    }

    private fun handleWindowInsets() {
        // Fungsi canggih untuk mencegah BottomNav terpotong oleh navigasi sistem HP
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Tambahkan margin bawah secara dinamis sesuai tinggi navigasi bar sistem + margin kustom 12dp
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_custom)
            }
            insets
        }
    }

    private fun setupDrawer() {
        val menu = binding.navigationView.menu
        val logoutItem = menu.findItem(R.id.nav_logout)
        val s = SpannableString(logoutItem.title)
        s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
        logoutItem.title = s
        logoutItem.iconTintList = ContextCompat.getColorStateList(this, R.color.error_red)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    performLogout()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) { updateDrawerHeader() }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    fun updateDrawerHeader() {
        val headerView: View = binding.navigationView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.iv_profile_header)
        val tvUsernameHeader: TextView = headerView.findViewById(R.id.tv_username_header)
        val prefManager = PreferenceManager(this)

        val name = prefManager.getMahasantriNama()
        tvUsernameHeader.text = name.ifEmpty { prefManager.getUsername() ?: "User" }

        val localUri = prefManager.getLocalProfileUri()
        val serverUrl = prefManager.getProfileUrl()
        
        val photoSource: Any = if (!localUri.isNullOrEmpty()) Uri.parse(localUri) else serverUrl ?: R.drawable.ic_profile

        Glide.with(this)
            .load(photoSource)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .into(ivProfileHeader)
    }

    private fun performLogout() {
        val prefManager = PreferenceManager(this)
        val token = prefManager.getToken() ?: ""
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        
        Toast.makeText(this, "Mengeluarkan akun...", Toast.LENGTH_SHORT).show()

        ApiConfig.getApiService().logout(formattedToken).enqueue(object : Callback<LogoutResponse> {
            override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                handleLocalLogout(prefManager)
            }
            override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                handleLocalLogout(prefManager)
            }
        })
    }

    private fun handleLocalLogout(prefManager: PreferenceManager) {
        prefManager.clear()
        Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
