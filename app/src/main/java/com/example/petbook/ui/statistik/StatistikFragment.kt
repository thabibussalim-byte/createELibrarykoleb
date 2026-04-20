package com.example.petbook.ui.statistik

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        //mengambil semua riwayat transaksi yang pernah dilakukan (buku apa saja yang sedang dipinjam,sudah dikembalikan atau masih pending)
        ApiConfig.getApiService().getHistoryByUser("Bearer $token", userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null && response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    setupChartAndDetails(data)
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {}
        })
    }

//
    private fun setupChartAndDetails(list: List<HistoryDataItem>) {
        // Hanya menghitung 3 status utama sesuai filter History
        val countDipinjam = list.count { it.status.lowercase() == "dipinjam" || it.status.lowercase() == "terlambat" }
        val countSelesai = list.count { it.status.lowercase() == "dikembalikan" || it.status.lowercase() == "selesai" }
        val countPending = list.count { it.status.lowercase() == "pending" }

        // Update Text Details di UI
        binding.tvCountDipinjam.text = countDipinjam.toString()
        binding.tvCountSelesai.text = countSelesai.toString()
        binding.tvCountPending.text = countPending.toString()

        // Setup Chart Entries (3 Batang saja)
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, countDipinjam.toFloat()))
        entries.add(BarEntry(1f, countSelesai.toFloat()))
        entries.add(BarEntry(2f, countPending.toFloat()))

        val dataSet = BarDataSet(entries, "Aktivitas")
        dataSet.colors = listOf(
            Color.parseColor("#60A5FA"), // Blue (Dipinjam)
            Color.parseColor("#34D399"), // Green (Selesai)
            Color.parseColor("#FBBF24")  // Amber (Pending)
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChartStatistik.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            
            // Konfigurasi Sumbu X (Label disesuaikan)
            val labels = arrayOf("Dipinjam", "Selesai", "Pending")
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 3
            
            // Konfigurasi Sumbu Y
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}