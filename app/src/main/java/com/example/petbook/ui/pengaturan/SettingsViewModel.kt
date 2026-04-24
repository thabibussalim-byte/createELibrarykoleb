package com.example.petbook.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.petbook.data.local.datastore.SettingPreferences
import kotlinx.coroutines.launch

class SettingsViewModel(private val pref: SettingPreferences) : ViewModel() {

    fun getThemeSettings() = pref.getThemeSetting().asLiveData()

    fun saveThemeSetting(isDarkModeActive: Boolean) {
        viewModelScope.launch {
            pref.saveThemeSetting(isDarkModeActive)
        }
    }

    fun getNotificationSettings() = pref.getNotificationSetting().asLiveData()

    fun saveNotificationSetting(isActive: Boolean) {
        viewModelScope.launch {
            pref.saveNotificationSetting(isActive)
        }
    }
}