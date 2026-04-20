package com.example.petbook.ui.statistik

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.api.model.HistoryResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentStatistikBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatistikFragment : Fragment() {

    private var _binding: FragmentStatistikBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatistikBinding.inflate(inflater, container, false)
        prefManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarStatistik.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        loadStatistikData()
    }

    private fun loadStatistikData() {
        val token = prefManager.getToken()
        val userId = prefManager.getUserId()
        if (token.isNullOrEmpty()) return

        val authHeader = "Bearer $token"

        // Mencoba mengambil history spesifik user
        ApiConfig.getApiService().getHistoryByUser(authHeader, userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    if (response.isSuccessful && !response.body()?.data.isNullOrEmpty()) {
                        val data = response.body()?.data ?: emptyList()
                        setupChartAndDetails(data)
                    } else {
                        // FALLBACK: Jika endpoint spesifik gagal, ambil semua lalu filter manual (seperti di HistoryFragment)
                        loadAllTransactionsFallback(authHeader, userId)
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) loadAllTransactionsFallback(authHeader, userId)
            }
        })
    }

    private fun loadAllTransactionsFallback(authHeader: String, userId: Int) {
        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val rawData = response.body()?.data ?: emptyList()
                    val filteredData = rawData.filter { it.userId == userId }
                    
                    if (filteredData.isEmpty()) {
                        Toast.makeText(requireContext(), "Tidak ada data untuk statistik", Toast.LENGTH_SHORT).show()
                    }
                    setupChartAndDetails(filteredData)
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                Log.e("Statistik", "Fallback Failure: ${t.message}")
            }
        })
    }

    private fun setupChartAndDetails(list: List<HistoryDataItem>) {
        val countDipinjam = list.count { it.status.lowercase() == "dipinjam" || it.status.lowercase() == "terlambat" }
        val countSelesai = list.count { it.status.lowercase() == "dikembalikan" || it.status.lowercase() == "selesai" }
        val countPending = list.count { it.status.lowercase() == "pending" }

        // Update Text Details
        binding.tvCountDipinjam.text = countDipinjam.toString()
        binding.tvCountSelesai.text = countSelesai.toString()
        binding.tvCountPending.text = countPending.toString()

        // Jika tidak ada data, jangan gambar grafik agar tidak error
        if (list.isEmpty()) return

        // Setup Chart
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, countDipinjam.toFloat()))
        entries.add(BarEntry(1f, countSelesai.toFloat()))
        entries.add(BarEntry(2f, countPending.toFloat()))

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#60A5FA"), // Blue
            Color.parseColor("#34D399"), // Green
            Color.parseColor("#FBBF24")  // Amber
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f // Membuat batang tidak terlalu lebar

        binding.barChartStatistik.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            
            // Konfigurasi Sumbu X
            val labels = arrayOf("Dipinjam", "Selesai", "Pending")
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 3
            xAxis.setDrawAxisLine(true)
            
            // Konfigurasi Sumbu Y
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f // Mulai dari angka 0
            axisRight.isEnabled = false
            
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}