package com.example.petbook.data.pref

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val SESSION_TIMEOUT = 30 * 60 * 1000 // 30 menit
        private const val LAST_ACTIVE_TIMESTAMP = "last_active_timestamp"
        private const val IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUser(id: Int, token: String, password: String, username: String, profileUrl: String) {
        val editor = prefs.edit()
        editor.putInt("user_id", id)
        editor.putString("token", token)
        editor.putString("username", username)
        editor.putString("password", password)
        editor.putString("profile_url", profileUrl)
        editor.putBoolean("is_logged_in", true)
        editor.putLong("login_timestamp", System.currentTimeMillis())
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putLong(LAST_ACTIVE_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }

    // Fungsi untuk memperbarui waktu aktivitas (panggil ini agar session diperpanjang)
    fun refreshSession() {
        if (prefs.getBoolean(IS_LOGGED_IN, false)) {
            prefs.edit().putLong(LAST_ACTIVE_TIMESTAMP, System.currentTimeMillis()).apply()
        }
    }

    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(IS_LOGGED_IN, false)
        if (!isLoggedIn) return false

        val lastActive = prefs.getLong(LAST_ACTIVE_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()


        if (currentTime - lastActive > SESSION_TIMEOUT) {
            clear()
            return false
        }

        refreshSession()
        return true
    }


    fun saveLocalProfileUri(uri: String) {
        prefs.edit().putString("local_profile_uri", uri).apply()
    }

    fun getLocalProfileUri(): String? = prefs.getString("local_profile_uri", null)

    fun saveMahasantriDetail(nama: String, jurusan: String, alamat: String, phone: String) {
        val editor = prefs.edit()
        editor.putString("mhs_nama", nama)
        editor.putString("mhs_jurusan", jurusan)
        editor.putString("mhs_alamat", alamat)
        editor.putString("mhs_phone", phone)
        editor.apply()
    }

    fun getMahasantriNama(): String = prefs.getString("mhs_nama", "") ?: ""
    fun getMahasantriJurusan(): String = prefs.getString("mhs_jurusan", "") ?: ""
    fun getMahasantriAlamat(): String = prefs.getString("mhs_alamat", "") ?: ""
    fun getMahasantriPhone(): String = prefs.getString("mhs_phone", "") ?: ""

    fun getUserId(): Int {
        var id = prefs.getInt("user_id", 0)
        if (id <= 0) {
            id = getIdFromToken()
            if (id > 0) {
                prefs.edit().putInt("user_id", id).apply()
            }
        }
        return id
    }

    private fun getIdFromToken(): Int {
        val token = getToken()
        if (token.isNullOrEmpty()) return -1
        try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
                val jsonObject = JSONObject(payload)
                if (jsonObject.has("id")) {
                    return jsonObject.getInt("id")
                }
            }
        } catch (e: Exception) {
            Log.e("PrefManager", "Gagal decode token: ${e.message}")
        }
        return -1
    }

    fun getUsername(): String? = prefs.getString("username", "")
    fun getProfileUrl(): String? = prefs.getString("profile_url", "")
    fun getToken(): String? = prefs.getString("token", "")
    fun getPassword(): String? = prefs.getString("password", "")

    // Simpan status terakhir transaksi untuk deteksi perubahan
    fun saveLastTransactionStatus(transactionId: Int, status: String) {
        prefs.edit().putString("trans_status_$transactionId", status).apply()
    }

    fun getLastTransactionStatus(transactionId: Int): String? {
        return prefs.getString("trans_status_$transactionId", null)
    }


    fun setStatusNotified(transactionId: Int, status: String) {
        prefs.edit().putBoolean("notified_${transactionId}_$status", true).apply()
    }

    fun isStatusNotified(transactionId: Int, status: String): Boolean {
        return prefs.getBoolean("notified_${transactionId}_$status", false)
    }

    // Tandai jika success screen sudah ditampilkan untuk buku tertentu
    fun setSuccessScreenShown(transactionId: Int) {
        prefs.edit().putBoolean("success_shown_$transactionId", true).apply()
    }

    fun isSuccessScreenShown(transactionId: Int): Boolean {
        return prefs.getBoolean("success_shown_$transactionId", false)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
