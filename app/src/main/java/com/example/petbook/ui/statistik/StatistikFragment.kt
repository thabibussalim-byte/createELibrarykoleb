package com.example.petbook.ui.statistik

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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class StatistikFragment : Fragment() {

    private var _binding: FragmentStatistikBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager
    
    private val monthLabels = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")

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

        binding.toolbarStatistik.setNavigationOnClickListener { findNavController().popBackStack() }
        loadStatistikData()
    }

    private fun loadStatistikData() {
        val token = prefManager.getToken()
        val userId = prefManager.getUserId()
        if (token.isNullOrEmpty()) return

        showLoading(true)

        ApiConfig.getApiService().getHistoryByUser("Bearer $token", userId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding == null) return
                
                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    val data = response.body()?.data!!
                    showLoading(false)
                    updateUI(data)
                } else {
                    loadAllTransactionsFallback("Bearer $token", userId)
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding == null) return
                loadAllTransactionsFallback("Bearer $token", userId)
            }
        })
    }

    private fun loadAllTransactionsFallback(token: String, userId: Int) {
        ApiConfig.getApiService().getAllTransactions(token).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding == null) return
                showLoading(false)
                
                if (response.isSuccessful) {
                    val allData = response.body()?.data ?: emptyList()
                    val userHistory = allData.filter { it.userId == userId }
                    updateUI(userHistory)
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    updateUI(emptyList()) 
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding == null) return
                showLoading(false)
                Toast.makeText(requireContext(), "Koneksi bermasalah", Toast.LENGTH_SHORT).show()
                updateUI(emptyList())
            }
        })
    }

    private fun updateUI(list: List<HistoryDataItem>) {
        updateSummary(list)
        setupMonthlyChart(list)
    }

    private fun updateSummary(list: List<HistoryDataItem>) {
        val dipinjam = list.count { it.status.contains("pinjam", true) || it.status.contains("lambat", true) }
        val dikembalikan = list.count { it.status.contains("kembali", true) || it.status.contains("selesai", true) }
        val pending = list.count { it.status.contains("pending", true) || it.status.contains("tunggu", true) }

        binding.tvCountDipinjam.text = dipinjam.toString()
        binding.tvCountSelesai.text = dikembalikan.toString()
        binding.tvCountPending.text = pending.toString()
    }

    private fun setupMonthlyChart(list: List<HistoryDataItem>) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthlyData = mutableMapOf<Int, Triple<Int, Int, Int>>()

        for (i in 0..11) monthlyData[i] = Triple(0, 0, 0)

        for (item in list) {
            try {
                val cleanDateStr = item.tglPinjam.take(10)
                val date = sdf.parse(cleanDateStr)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    val month = cal.get(Calendar.MONTH)
                    
                    val current = monthlyData[month] ?: Triple(0, 0, 0)
                    val status = item.status.lowercase()
                    
                    val updated = when {
                        status.contains("pinjam") || status.contains("lambat") -> current.copy(first = current.first + 1)
                        status.contains("kembali") || status.contains("selesai") -> current.copy(second = current.second + 1)
                        status.contains("pending") || status.contains("tunggu") -> current.copy(third = current.third + 1)
                        else -> current
                    }
                    monthlyData[month] = updated
                }
            } catch (e: Exception) {
                Log.e("Statistik", "Gagal parse tanggal: ${item.tglPinjam}")
            }
        }

        val entriesDipinjam = ArrayList<BarEntry>()
        val entriesSelesai = ArrayList<BarEntry>()
        val entriesPending = ArrayList<BarEntry>()

        for (i in 0..11) {
            val d = monthlyData[i] ?: Triple(0, 0, 0)
            entriesDipinjam.add(BarEntry(i.toFloat(), d.first.toFloat()))
            entriesSelesai.add(BarEntry(i.toFloat(), d.second.toFloat()))
            entriesPending.add(BarEntry(i.toFloat(), d.third.toFloat()))
        }

        val set1 = BarDataSet(entriesDipinjam, "Dipinjam").apply { 
            color = "#60A5FA".toColorInt()
            setDrawValues(false) 
        }
        val set2 = BarDataSet(entriesSelesai, "Selesai").apply {
        color = "#34D399".toColorInt()
            setDrawValues(false) 
        }
        val set3 = BarDataSet(entriesPending, "Pending").apply { 
            color = "#FBBF24".toColorInt()
            setDrawValues(false) 
        }

        val barData = BarData(set1, set2, set3)
        
        val groupSpace = 0.34f
        val barSpace = 0.02f
        val barWidth = 0.2f 
        barData.barWidth = barWidth

        binding.barChartStatistik.apply {
            clear()
            
            data = barData
            description.isEnabled = false
            legend.isEnabled = true

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(monthLabels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = 12f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toInt().toString()
                }
            }
            
            axisRight.isEnabled = false

            setFitBars(true)
            
            groupBars(0f, groupSpace, barSpace)
            
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH).toFloat()
            setVisibleXRangeMaximum(4f) 
            moveViewToX(currentMonth)
            
            animateY(800)
            invalidate()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
