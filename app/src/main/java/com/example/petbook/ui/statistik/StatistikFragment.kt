package com.example.petbook.ui.statistik

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.R
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.data.api.model.HistoryResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentStatistikBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.graphics.toColorInt

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
        loadStatistikData()
    }

    private fun loadStatistikData() {
        val token = prefManager.getToken()
        val userId = prefManager.getUserId()
        if (token.isNullOrEmpty()) return

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        showLoading(true)

        ApiConfig.getApiService().getAllTransactions(authHeader).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (_binding != null) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val rawData = response.body()?.data ?: emptyList()
                        val filteredData = rawData.filter { it.userId == userId }
                        setupChartAndDetails(filteredData)
                    } else {
                        handleError("Gagal memuat data statistik: ${response.message()}")
                    }
                }
            }
            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                if (_binding != null) {
                    showLoading(false)
                    handleError("Koneksi gagal: ${t.message}")
                }
            }
        })
    }

    private fun handleError(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun setupChartAndDetails(list: List<HistoryDataItem>) {
        val countDipinjam = list.count { it.status.lowercase() == "dipinjam" || it.status.lowercase() == "terlambat" }
        val countSelesai = list.count { it.status.lowercase() == "dikembalikan" || it.status.lowercase() == "selesai" }
        val countPending = list.count { it.status.lowercase() == "pending" }

        binding.tvCountDipinjam.text = countDipinjam.toString()
        binding.tvCountSelesai.text = countSelesai.toString()
        binding.tvCountPending.text = countPending.toString()

        if (list.isEmpty()) {
            binding.barChartStatistik.clear()
            return
        }

        val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, countDipinjam.toFloat()))
        entries.add(BarEntry(1f, countSelesai.toFloat()))
        entries.add(BarEntry(2f, countPending.toFloat()))

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(
            "#60A5FA".toColorInt(),
            "#34D399".toColorInt(),
            "#FBBF24".toColorInt()
        )
        dataSet.valueTextColor = textColor
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = value.toInt().toString()
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        binding.barChartStatistik.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(arrayOf("Dipinjam", "Selesai", "Pending"))
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 3
                this.textColor = textColor
            }
            
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                granularity = 1f
                this.textColor = textColor
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toInt().toString()
                }
            }
            
            axisRight.isEnabled = false
            setFitBars(true)
            animateY(1000)
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
