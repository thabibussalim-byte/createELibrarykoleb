package com.example.elibrarypetik.ui.mainview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Contoh fungsi untuk mengatur status loading
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // Nanti di sini bisa ditambahkan logika untuk session user atau data profil

}