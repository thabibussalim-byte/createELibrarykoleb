package com.example.petbook.ui.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.*
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailpeminjamanBinding
import com.example.petbook.utils.StatusCheckWorker
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DetailpeminjamanFragment : Fragment() {

    private var _binding: FragmentDetailpeminjamanBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailpeminjamanBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val book = arguments?.let {
            BundleCompat.getParcelable(it, "book", BookItem::class.java)
        }
        val writerName = arguments?.getString("book_writer")

        setupBookPreview(book, writerName)
        setupMaterialDatePicker()
        setupAutoCalculateReturnDate()

        binding.btnPinjamFinal.setOnClickListener {
            if (book == null) {
                Toast.makeText(requireContext(), "Buku tidak ditemukan", Toast.LENGTH_SHORT).show()
            } else if (book.stok <= 0) {
                Toast.makeText(requireContext(), "Maaf, stok buku sedang kosong", Toast.LENGTH_SHORT).show()
            } else {
                checkHistoryAndBorrow(book)
            }
        }
    }

    private fun checkHistoryAndBorrow(book: BookItem) {
        val currentUserId = prefManager.getUserId()
        if (currentUserId <= 0) {
            Toast.makeText(requireContext(), "Sesi habis, silakan login kembali", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarBorrow.visibility = View.VISIBLE
        binding.btnPinjamFinal.isEnabled = false

        val userToken = prefManager.getToken() ?: ""
        val authHeader = if (userToken.startsWith("Bearer ")) userToken else "Bearer $userToken"

        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (response.isSuccessful) {
                    val rawData = response.body()?.data ?: emptyList()
                    val userHistory = rawData.filter { it.userId == currentUserId }
                    validateHistoryAndProceed(userHistory, book)
                } else {
                    handleError("Gagal memvalidasi riwayat: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                handleError("Kesalahan jaringan saat validasi")
            }
        })
    }

    private fun validateHistoryAndProceed(history: List<HistoryDataItem>, book: BookItem) {
        val currentUserId = prefManager.getUserId()
        val activeStatuses = listOf("dipinjam", "pending", "telat")
        val hasUnpaidFine = history.any {
            (it.denda ?: 0) > 0 && it.status.lowercase() == "telat"
        }
        val hasActiveLoan = history.any { 
            it.status.lowercase() in activeStatuses && it.userId == currentUserId
        }
        if (hasActiveLoan) {
            handleError("Gagal! Anda masih memiliki peminjaman aktif atau pending.")
        } else if (hasUnpaidFine) {
            handleError("Gagal! Anda memiliki denda yang belum dibayar. Silakan lunasKotlini denda terlebih dahulu.")
        } else {
            performBorrowAction(book.id)
        }
    }

    private fun handleError(message: String) {
        if (_binding != null) {
            binding.progressBarBorrow.visibility = View.GONE
            binding.btnPinjamFinal.isEnabled = true
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun performBorrowAction(bookId: Int) {
        val lamaPinjamStr = binding.etLamaPinjam.text.toString()

        if (lamaPinjamStr.isEmpty()) {
            binding.etLamaPinjam.error = "Harap isi durasi"
            binding.progressBarBorrow.visibility = View.GONE
            binding.btnPinjamFinal.isEnabled = true
            return
        }

        val days = lamaPinjamStr.toIntOrNull() ?: 0
        if (days > 14) {
            Toast.makeText(requireContext(), "Maksimal peminjaman 14 hari", Toast.LENGTH_LONG).show()
            binding.progressBarBorrow.visibility = View.GONE
            binding.btnPinjamFinal.isEnabled = true
            return
        }

        val userToken = prefManager.getToken() ?: ""
        val authHeader = if (userToken.startsWith("Bearer ")) userToken else "Bearer $userToken"

        val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tglPinjam = sdfApi.format(calendar.time)
        
        val returnCalendar = calendar.clone() as Calendar
        returnCalendar.add(Calendar.DAY_OF_YEAR, days)
        val tglKembali = sdfApi.format(returnCalendar.time)

        val request = BorrowRequest(tglPinjam, tglKembali, bookId)

        ApiConfig.getApiService().createBorrow(authHeader, request).enqueue(object : Callback<BorrowResponse> {
            override fun onResponse(call: Call<BorrowResponse>, response: Response<BorrowResponse>) {
                if (_binding != null) {
                    binding.progressBarBorrow.visibility = View.GONE
                    binding.btnPinjamFinal.isEnabled = true
                    
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(requireContext(), "Permintaan peminjaman berhasil!", Toast.LENGTH_SHORT).show()
                        startStatusCheckWorker()
                        findNavController().navigate(R.id.historyFragment)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Borrow", "Gagal: $errorBody")
                        Toast.makeText(requireContext(), "Gagal memproses peminjaman", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {
                if (_binding != null) {
                    binding.progressBarBorrow.visibility = View.GONE
                    binding.btnPinjamFinal.isEnabled = true
                    Toast.makeText(requireContext(), "Koneksi Bermasalah", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupMaterialDatePicker() {
        updateDateLabel()
        binding.etTglPinjam.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pilih Tanggal Pinjam")
                .setSelection(calendar.timeInMillis)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                calendar.timeInMillis = selection
                updateDateLabel()
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun startStatusCheckWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data = Data.Builder().putLong("start_time", System.currentTimeMillis()).build()
        val statusWorkRequest = PeriodicWorkRequestBuilder<StatusCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .addTag("StatusCheckWorker")
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork("BorrowStatusCheck", ExistingPeriodicWorkPolicy.REPLACE, statusWorkRequest)
    }

    private fun setupBookPreview(book: BookItem?, writerName: String?) {
        if (book != null) {
            binding.tvBorrowTitle.text = book.judulBuku
            binding.tvBorrowAuthor.text = writerName ?: "Penulis Anonim"

            if (book.foto.isNotEmpty()) {
                Glide.with(this)
                    .load(book.foto)
                    .placeholder(R.drawable.bintang)
                    .error(R.drawable.bintang)
                    .into(binding.ivBorrowPreview)
            } else {
                binding.ivBorrowPreview.setImageResource(R.drawable.bintang)
            }
        }
    }

    private fun updateDateLabel() {
        val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.etTglPinjam.setText(sdfDisplay.format(calendar.time))
        calculateReturnDate()
    }

    private fun setupAutoCalculateReturnDate() {
        binding.etLamaPinjam.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { calculateReturnDate() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun calculateReturnDate() {
        val lamaPinjamStr = binding.etLamaPinjam.text.toString()
        if (lamaPinjamStr.isNotEmpty()) {
            val days = try { lamaPinjamStr.toInt() } catch (e: Exception) { 0 }
            val returnCalendar = calendar.clone() as Calendar
            returnCalendar.add(Calendar.DAY_OF_YEAR, days)
            val sdfDisplay = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
            binding.tvTglKembaliEst.text = sdfDisplay.format(returnCalendar.time)
        } else {
            binding.tvTglKembaliEst.text = "-"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
