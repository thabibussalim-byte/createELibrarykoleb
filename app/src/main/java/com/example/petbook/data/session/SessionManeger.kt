package com.example.petbook.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        const val IS_LOGIN = "is_login"
        const val USER_TOKEN = "user_token"
        const val LOGIN_TIMESTAMP = "login_timestamp"
        const val SESSION_TIMEOUT = 30 * 60 * 1000
    }

    // Simpan status login
    fun saveLoginSession(token: String) {
        val editor = prefs.edit()
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(USER_TOKEN, token)
        editor.putLong(LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }

    // Cek apakah sudah login dan sesi masih valid (kurang dari 30 menit)
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(IS_LOGIN, false)
        if (!isLoggedIn) return false

        val loginTime = prefs.getLong(LOGIN_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        // Jika sudah lebih dari 30 menit, anggap sesi berakhir
        if (currentTime - loginTime > SESSION_TIMEOUT) {
            logout() // Opsional: hapus session jika sudah timeout
            return false
        }

        return true
    }


    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
