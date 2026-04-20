package com.example.petbook.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.LogoutResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.ActivityMainBinding
import com.example.petbook.ui.auth.AuthActivity
import com.example.petbook.utils.ReminderWorker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifikasi diizinkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifikasi tidak diizinkan", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()
        setupWorkManager()

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

        // Custom BottomNav behavior to clear stack
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != binding.bottomNav.selectedItemId) {
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(false)
                    .setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
                    .build()
                navController.navigate(item.itemId, null, options)
            } else {
                navController.popBackStack(item.itemId, false)
            }
            true
        }

        // PANGGIL UPDATE HEADER SAAT START
        updateDrawerHeader()

        binding.navigationView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_logout) {
                performLogout()
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag("ReminderWorker")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DueReminderWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    private fun performLogout() {
        val prefManager = PreferenceManager(this)
        val token = prefManager.getToken() ?: ""
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

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
        WorkManager.getInstance(this).cancelAllWorkByTag("ReminderWorker")
        Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // FUNGSI UNTUK UPDATE FOTO DAN NAMA DI DRAWER
    fun updateDrawerHeader() {
        val headerView: View = binding.navigationView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.iv_profile_header)
        val tvUsernameHeader: TextView = headerView.findViewById(R.id.tv_username_header)
        val prefManager = PreferenceManager(this)

        tvUsernameHeader.text = prefManager.getUsername() ?: "User"

        val photoUrl = prefManager.getProfileUrl()
        Glide.with(this)
            .load(photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(ivProfileHeader)
    }

    // Pastikan header di-update setiap kali activity kembali aktif (misal dari Profile)
    override fun onResume() {
        super.onResume()
        updateDrawerHeader()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}