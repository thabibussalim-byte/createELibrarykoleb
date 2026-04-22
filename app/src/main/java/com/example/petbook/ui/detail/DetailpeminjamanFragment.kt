package com.example.petbook.ui.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.BorrowRequest
import com.example.petbook.data.api.model.BorrowResponse
import com.example.petbook.data.api.model.HistoryResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentDetailpeminjamanBinding
import com.example.petbook.utils.StatusCheckWorker
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
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
        val token = prefManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Sesi habis, login kembali", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarBorrow.visibility = View.VISIBLE
        binding.btnPinjamFinal.isEnabled = false

        val authHeader = "Bearer $token"
        
        // Cek history transaksi user secara keseluruhan
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (response.isSuccessful) {
                    val history = response.body()?.data ?: emptyList()
                    
                    // Cek apakah ada status 'dipinjam' atau 'pending' di seluruh list history
                    val hasActiveLoan = history.any { 
                        it.status.lowercase() == "dipinjam" || it.status.lowercase() == "pending" 
                    }

                    if (hasActiveLoan) {
                        binding.progressBarBorrow.visibility = View.GONE
                        binding.btnPinjamFinal.isEnabled = true
                        Toast.makeText(requireContext(), "Gagal! Anda masih memiliki peminjaman aktif atau pending.", Toast.LENGTH_LONG).show()
                    } else {
                        // Jika tidak ada status dipinjam/pending di history, lanjutkan peminjaman
                        performBorrowAction(book.id)
                    }
                } else {
                    binding.progressBarBorrow.visibility = View.GONE
                    binding.btnPinjamFinal.isEnabled = true
                    Toast.makeText(requireContext(), "Gagal memvalidasi riwayat peminjaman", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                binding.progressBarBorrow.visibility = View.GONE
                binding.btnPinjamFinal.isEnabled = true
                Toast.makeText(requireContext(), "Terjadi kesalahan koneksi", Toast.LENGTH_SHORT).show()
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

        val token = prefManager.getToken() ?: ""
        val authHeader = "Bearer $token"

        val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tglPinjam = sdfApi.format(calendar.time)
        
        val returnCalendar = calendar.clone() as Calendar
        returnCalendar.add(Calendar.DAY_OF_YEAR, days)
        val tglKembali = sdfApi.format(returnCalendar.time)

        val request = BorrowRequest(tglPinjam, tglKembali, bookId)

        ApiConfig.getApiService().createBorrow(authHeader, request).enqueue(object : Callback<BorrowResponse> {
            override fun onResponse(call: Call<BorrowResponse>, response: Response<BorrowResponse>) {
                binding.progressBarBorrow.visibility = View.GONE
                binding.btnPinjamFinal.isEnabled = true
                
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(requireContext(), "Permintaan peminjaman berhasil \n Silakan tunggu konfirmasi admin!", Toast.LENGTH_SHORT).show()
                    startStatusCheckWorker()
                    findNavController().navigate(R.id.historyFragment)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorResponse = Gson().fromJson(errorBody, BorrowResponse::class.java)
                            errorResponse.errors?.firstOrNull()?.message ?: errorResponse.message
                        } catch (e: Exception) { "Terjadi kesalahan server" }
                    } else { response.body()?.message ?: "Gagal memproses" }
                    
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<BorrowResponse>, t: Throwable) {
                binding.progressBarBorrow.visibility = View.GONE
                binding.btnPinjamFinal.isEnabled = true
                Toast.makeText(requireContext(), "Koneksi Bermasalah", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startStatusCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putLong("start_time", System.currentTimeMillis())
            .build()

        val statusWorkRequest = PeriodicWorkRequestBuilder<StatusCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .addTag("StatusCheckWorker")
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "BorrowStatusCheck",
            ExistingPeriodicWorkPolicy.REPLACE,
            statusWorkRequest
        )
    }

    private fun setupBookPreview(book: BookItem?, writerName: String?) {
        if (book != null) {
            binding.tvBorrowTitle.text = book.judulBuku
            binding.tvBorrowAuthor.text = writerName ?: "Penulis Anonim"
            Glide.with(this).load(book.foto).placeholder(R.drawable.bintang).into(binding.ivBorrowPreview)
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
