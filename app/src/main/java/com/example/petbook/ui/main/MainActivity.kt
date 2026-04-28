package com.example.petbook.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.LogoutResponse
import com.example.petbook.data.di.Injection
import com.example.petbook.data.local.datastore.SettingPreferences
import com.example.petbook.data.local.datastore.ViewModelFactory
import com.example.petbook.data.local.datastore.dataStore
import com.example.petbook.data.local.room.AppDatabase
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.data.session.SessionManager
import com.example.petbook.databinding.ActivityMainBinding
import com.example.petbook.ui.auth.AuthActivity
import com.example.petbook.ui.pengaturan.SettingsViewModel
import com.example.petbook.utils.NotificationHelper
import com.example.petbook.utils.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import androidx.core.content.edit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyThemeSettings()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment != null) {
            navController = navHostFragment.navController
        } else {
            finish()
            return
        }

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.bookFragment, R.id.historyFragment, R.id.profileFragment),
            binding.drawerLayout
        )

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        setupDrawer()
        handleWindowInsets()
        setupDestinationListener()
        updateDrawerHeader()
        setupPeriodicWork()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val target = intent?.getStringExtra("target_page")
        if (target != null) {
            when (target) {
                NotificationHelper.TARGET_HISTORY -> {
                    navController.navigate(R.id.historyFragment)
                }
                NotificationHelper.TARGET_CATALOG -> {
                    navController.navigate(R.id.bookFragment)
                }
            }
        }
    }

    private fun setupPeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("ReminderWorkTag")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PetbookReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        val pref = getSharedPreferences("worker_prefs", MODE_PRIVATE)
        if (pref.getLong("start_time", 0L) == 0L) {
            pref.edit { putLong("start_time", System.currentTimeMillis()) }
        }
    }

    private fun applyThemeSettings() {
        val pref = SettingPreferences.getInstance(dataStore)
        val factory = ViewModelFactory(Injection.provideRepository(this), pref)
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        settingsViewModel.getThemeSettings().observe(this) { isDarkModeActive ->
            AppCompatDelegate.setDefaultNightMode(if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupDestinationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            window.statusBarColor = ContextCompat.getColor(this, R.color.accent_blue)
            if (destination.id in appBarConfiguration.topLevelDestinations) {
                supportActionBar?.title = "PeTbook"
                binding.bottomNav.visibility = View.VISIBLE
            } else {
                supportActionBar?.title = destination.label
                binding.bottomNav.visibility = View.GONE
            }
        }
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_custom)
            }
            insets
        }
    }

    private fun setupDrawer() {
        val menu = binding.navigationView.menu
        val logoutItem = menu.findItem(R.id.nav_logout)
        if (logoutItem != null) {
            val s = SpannableString(logoutItem.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            logoutItem.title = s
            logoutItem.iconTintList = ContextCompat.getColorStateList(this, R.color.error_red)
        }
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> { showLogoutConfirmation(); binding.drawerLayout.closeDrawer(GravityCompat.START); true }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this).setTitle("Logout").setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya, Keluar") { _, _ -> performLogout() }
            .setNegativeButton("Batal", null).show()
    }

    fun updateDrawerHeader() {
        val headerView: View = binding.navigationView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.iv_profile_header)
        val tvUsernameHeader: TextView = headerView.findViewById(R.id.tv_username_header)
        val prefManager = PreferenceManager(this)
        val name = prefManager.getMahasantriNama()
        tvUsernameHeader.text = name.ifEmpty { prefManager.getUsername() ?: "User" }
        val localUri = prefManager.getLocalProfileUri()
        val photoSource: Any = if (!localUri.isNullOrEmpty()) localUri.toUri() else prefManager.getProfileUrl() ?: R.drawable.ic_profile

        Glide.with(this)
            .load(photoSource)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .into(ivProfileHeader)

        ivProfileHeader.setOnClickListener {
            showImagePreview(photoSource)
        }
    }

    private fun showImagePreview(photoSource: Any) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null)
        val ivFullImage = dialogView.findViewById<ImageView>(R.id.iv_full_image)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .create()

        Glide.with(this)
            .load(photoSource)
            .placeholder(R.drawable.ic_profile)
            .into(ivFullImage)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performLogout() {
        val prefManager = PreferenceManager(this)
        val sessionManager = SessionManager(this)
        val token = prefManager.getToken() ?: ""
        val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        ApiConfig.getApiService().logout(formattedToken).enqueue(object : Callback<LogoutResponse> {
            override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) { handleLocalLogout(prefManager, sessionManager) }
            override fun onFailure(call: Call<LogoutResponse>, t: Throwable) { handleLocalLogout(prefManager, sessionManager) }
        })
    }

    private fun handleLocalLogout(prefManager: PreferenceManager, sessionManager: SessionManager) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(this@MainActivity).clearAllTables()
            prefManager.clear()
            sessionManager.logout()
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag("ReminderWorkTag")
            withContext(Dispatchers.Main) {
                val intent = Intent(this@MainActivity, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
