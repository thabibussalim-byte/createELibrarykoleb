package com.example.petbook.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        const val IS_LOGIN = "is_login"
        const val USER_TOKEN = "user_token"
    }

    // Simpan status login
    fun saveLoginSession(token: String) {
        val editor = prefs.edit()
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    // Cek apakah sudah login
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGIN, false)
    }

    // Logout
    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
