package com.example.petbook.data.pref

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveLogin(token: String, username: String) {
        val editor = prefs.edit()
        editor.putString("token", token)
        editor.putString("username", username)
        editor.putBoolean("is_logged_in", true)
        editor.apply()
    }

    fun saveUser(id: Int, username: String, profil: String, role: String){
        val editor = prefs.edit()
        editor.putInt("id", id)
        editor.putString("username", username)
        editor.putString("profile_url", profil)
        editor.putString("role", role)
        editor.apply()
    }



    fun getId(): Int = prefs.getInt("id", 0)
    fun getUsername(): String? = prefs.getString("username", "")
    fun getProfileUrl(): String? = prefs.getString("profile_url", "")
    fun getToken(): String? = prefs.getString("token", "")
    fun getRole(): String? = prefs.getString("role", "")
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}