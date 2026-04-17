package com.example.petbook.data.pref

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(token: String, username: String, profileUrl: String) {
        val editor = prefs.edit()
        editor.putString("token", token)
        editor.putString("username", username)
        editor.putString("profile_url", profileUrl)
        editor.putBoolean("is_logged_in", true)
        editor.apply()
    }

    fun getUsername(): String? = prefs.getString("username", "")
    fun getProfileUrl(): String? = prefs.getString("profile_url", "")
    fun getToken(): String? = prefs.getString("token", "")
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}