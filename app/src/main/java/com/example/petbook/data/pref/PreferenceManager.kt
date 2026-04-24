package com.example.petbook.data.pref

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(id: Int, token: String, username: String, password: String, profileUrl: String) {
        val editor = prefs.edit()
        editor.putInt("user_id", id)
        editor.putString("token", token)
        editor.putString("username", username)
        editor.putString("password", password)
        editor.putString("profile_url", profileUrl)
        editor.putBoolean("is_logged_in", true)
        editor.apply()
    }

    // Fungsi untuk menyimpan URI foto secara lokal
    fun saveLocalProfileUri(uri: String) {
        prefs.edit().putString("local_profile_uri", uri).apply()
    }

    fun getLocalProfileUri(): String? = prefs.getString("local_profile_uri", null)

    //fungsi untuk menampilkan informasi mahasantri di halaman profile
    fun saveMahasantriDetail(id: Int, nama: String, jurusan: String, alamat: String, phone: String) {
        val editor = prefs.edit()
        editor.putInt("mhs_id", id) // SIMPAN ID MAHASANTRI
        editor.putString("mhs_nama", nama)
        editor.putString("mhs_jurusan", jurusan)
        editor.putString("mhs_alamat", alamat)
        editor.putString("mhs_phone", phone)
        editor.apply()
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
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
