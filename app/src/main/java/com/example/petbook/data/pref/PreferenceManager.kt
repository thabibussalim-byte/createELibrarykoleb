package com.example.petbook.data.pref

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import androidx.core.content.edit

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(id: Int, token: String, username: String, password: String, profileUrl: String) {
        prefs.edit {
            putInt("user_id", id)
            putString("token", token)
            putString("username", username)
            putString("password", password)
            putString("profile_url", profileUrl)
            putBoolean("is_logged_in", true)
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    @SuppressLint("UseKtx")
    fun saveLocalProfileUri(uri: String) {
        prefs.edit().putString("local_profile_uri", uri).apply()
    }

    fun getLocalProfileUri(): String? = prefs.getString("local_profile_uri", null)

    fun saveMahasantriDetail(id: Int, nama: String, jurusan: String, alamat: String, phone: String) {
        prefs.edit {
            putInt("mhs_id", id)
            putString("mhs_nama", nama)
            putString("mhs_jurusan", jurusan)
            putString("mhs_alamat", alamat)
            putString("mhs_phone", phone)
        }
    }

    fun getMahasantriId(): Int = prefs.getInt("mhs_id", 0)
    fun getMahasantriNama(): String = prefs.getString("mhs_nama", "") ?: ""
    fun getMahasantriJurusan(): String = prefs.getString("mhs_jurusan", "") ?: ""
    fun getMahasantriAlamat(): String = prefs.getString("mhs_alamat", "") ?: ""
    fun getMahasantriPhone(): String = prefs.getString("mhs_phone", "") ?: ""

    fun getUserId(): Int {
        var id = prefs.getInt("user_id", 0)
        if (id <= 0) {
            id = getIdFromToken()
            if (id > 0) {
                prefs.edit { putInt("user_id", id) }
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
    fun setStatusSeen(transactionId: Int, status: String) {
        prefs.edit { putBoolean("seen_${transactionId}_$status", true) }
    }

    fun isStatusSeen(transactionId: Int, status: String): Boolean {
        return prefs.getBoolean("seen_${transactionId}_$status", false)
    }

    fun clear() {
        prefs.edit {
            val allEntries = prefs.all
            for (entry in allEntries) {
                val key = entry.key
                if (!key.startsWith("seen_")) {
                    remove(key)
                }
            }
        }
    }
}
