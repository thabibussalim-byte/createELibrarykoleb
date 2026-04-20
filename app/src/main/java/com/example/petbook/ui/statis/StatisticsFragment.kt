package com.example.petbook.ui.statis

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

import com.example.petbook.databinding.FragmentStatistikBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import androidx.core.graphics.toColorInt

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatistikBinding? = null
    private val binding get() = _binding!!


    private val listHari = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    private val listBulan = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val listTahun = listOf("2025", "2026")


    private var currentIdx = 0
    private var currentDayIndex = 0
    private var currentMonthIndex = 0
    private var currentYearIndex = 1


    // Contoh Data Harian (Senin - Minggu)
    private val allDataHarian = listOf(
        listOf(2f, 1f, 0f, 1f, 1f), // Data Senin
        listOf(3f, 0f, 1f, 2f, 0f), // Data Selasa
        listOf(5f, 2f, 0f, 4f, 1f), // Data Rabu
        listOf(8f, 2f, 1f, 5f, 3f), // Data Kamis
        listOf(4f, 4f, 2f, 2f, 1f), // Data Jumat
        listOf(3f, 2f, 1f, 0f, 1f), // Data Sabtu
        listOf(2f, 1f, 0f, 1f, 1f), // Data Minggu
    )

    // Contoh Data Bulanan (Januari - Desember)
    private val allDataBulanan = listOf(
        listOf(5f, 3f, 2f, 1f, 2f), // Data Januari
        listOf(8f, 2f, 1f, 5f, 3f), // Data Februari
        listOf(4f, 4f, 2f, 2f, 1f), // Data Maret
        listOf(3f, 2f, 1f, 0f, 1f), // Data April)
        listOf(2f, 1f, 0f, 1f, 1f), // Data Mei
        listOf(3f, 0f, 1f, 2f, 0f), // Data Juni
        listOf(5f, 2f, 0f, 4f, 1f), // Data Juli
        listOf(8f, 2f, 1f, 5f, 3f), // Data Agustus
        listOf(4f, 4f, 2f, 2f, 1f), // Data September
        listOf(3f, 2f, 1f, 0f, 1f), // Data Oktober
        listOf(2f, 1f, 0f, 1f, 1f), // Data November
        listOf(3f, 0f, 1f, 2f, 0f), // Data Desember
    )

    private val allDataTahunan = listOf(
        listOf(80f, 20f, 10f, 50f, 30f), // Data Tahun 2025
        listOf(40f, 40f, 20f, 20f, 10f) // Data Tahun 2026
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentStatistikBinding.inflate(inflater, container, false)
        return binding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        setupSpinner()

//
//        binding.btnNext.setOnClickListener {
//            val mode = binding.spinnerTimeFrame.text.toString()
//            if (mode == "Tahunan") {
//                if (currentYearIndex < listTahun.size - 1) {
//                    currentYearIndex++
//                    updateUI()
//                }
//            } else {
//                binding.btnNext.setOnClickListener {
//                    val limit = if (binding.spinnerTimeFrame.text.toString() == "Bulanan")
//                        listBulan.size else listHari.size
//                    if (currentIdx < limit - 1) {
//                        currentIdx++
//                        updateUI()
//                    }
//                }
//
//
//                binding.btnPrev.setOnClickListener {
//                    if (currentIdx > 0) {
//                        currentIdx--
//                        updateUI()
//                    }
//                }
//            }
//        }
//
//
//        binding.btnPrev.setOnClickListener {
//            val mode = binding.spinnerTimeFrame.text.toString()
//            if (mode == "Tahunan") {
//                if (currentYearIndex > 0) {
//                    currentYearIndex--
//                    updateUI()
//                }
//            } else {
//                binding.btnNext.setOnClickListener {
//                    val limit = if (binding.spinnerTimeFrame.text.toString() == "Bulanan")
//                        listBulan.size else listHari.size
//                    if (currentIdx < limit - 1) {
//                        currentIdx++
//                        updateUI()
//                    }
//                }
//
//
//                binding.btnPrev.setOnClickListener {
//                    if (currentIdx > 0) {
//                        currentIdx--
//                        updateUI()
//                    }
//                }
//            }
//        }
//
//
//
//
//        updateUI()
//    }
//
//    private fun setupSpinner() {
//        val items = arrayOf("Harian", "Bulanan", "Tahunan")
//        val adapter = ArrayAdapter(requireContext(),
//            android.R.layout.simple_list_item_1, items)
//        binding.spinnerTimeFrame.setAdapter(adapter)
//
//        binding.spinnerTimeFrame.setOnItemClickListener { _, _, _, _ ->
//            currentIdx = 0 //
//            updateUI()
//        }
//    }
//
//    private fun updateUI() {
//        val selectedMode = binding.spinnerTimeFrame.text.toString()
//
//        when (selectedMode) {
//            "Harian" -> {
//                updateContent(listHari[currentDayIndex],
//                    allDataHarian[currentDayIndex])
//            }
//
//            "Bulanan" -> {
//                updateContent(listBulan[currentMonthIndex],
//                    allDataBulanan[currentMonthIndex])
//            }
//
//            "Tahunan" -> {
//                val teksTahun = listTahun[currentYearIndex]
//                val dataTahun = allDataTahunan[currentYearIndex]
//                updateContent(teksTahun, dataTahun)
//            }
//        }
//    }
//    private fun updateContent(periodeTeks: String, data: List<Float>) {
//        binding.tvCurrentPeriod.text = periodeTeks
//
//        updateChart(data)
//
//        binding.apply {
//            tvValueDisetujui.text = data[0].toInt().toString()
//            tvValueDitolak.text = data[1].toInt().toString()
//            tvValueDibatalkan.text = data[2].toInt().toString()
//            tvValueDikembalikan.text = data[3].toInt().toString()
//            tvValueBelumKembali.text = data[4].toInt().toString()
//        }
//    }
//
//    private fun updateChart(dataList: List<Float>) {
//        val entries = ArrayList<BarEntry>()
//
//        // 1. Memasukkan data ke dalam objek BarEntry
//        dataList.forEachIndexed { index, value ->
//            entries.add(BarEntry(index.toFloat(), value))
//        }
//
//        // 2. Konfigurasi DataSet (Batang Grafik)
//        val dataSet = BarDataSet(entries, "Statistik e-Library")
//
//        // Warna batang sesuai urutan kategori di legend
//        dataSet.colors = listOf(
//            "#A0F1FF".toColorInt(), // Cyan (Disetujui)
//            "#B1F375".toColorInt(), // Hijau (Ditolak)
//            "#FFEB55".toColorInt(), // Kuning (Dibatalkan)
//            "#FFB067".toColorInt(), // Oranye Muda (Dikembalikan)
//            "#FF8D3F".toColorInt()  // Oranye Tua (Belum Kembali)
//        )
//
//        // Hilangkan angka nilai di atas batang agar tampilan bersih
//        dataSet.setDrawValues(false)
//
//        // 3. Konfigurasi BarData
//        val barData = BarData(dataSet)
//        barData.barWidth = 0.5f // Mengatur ketebalan batang agar tidak terlalu lebar
//
//        // 4. Konfigurasi BarChart (Sumbu & Tampilan)
//        binding.barChart.apply {
//            data = barData
//
//            // Menghilangkan deskripsi dan legend bawaan library
//            description.isEnabled = false
//            legend.isEnabled = false
//
//            // Konfigurasi Sumbu X (Bawah)
//            xAxis.apply {
//                isEnabled = false // MENGHILANGKAN ANGKA 0, 1, 2, 3, 4
//                setDrawGridLines(false)
//            }
//
//            // Konfigurasi Sumbu Y Kiri
//            axisLeft.apply {
//                textColor = Color.WHITE
//                axisMinimum = 0f
//                setDrawGridLines(false)
//                granularity = 1f // Memastikan angka sumbu Y selalu bulat
//            }
//
//            // Mematikan Sumbu Y Kanan
//            axisRight.isEnabled = false
//
//            // Tambahkan animasi agar batang muncul dari bawah ke atas
//            animateY(1000)
//
//            // Refresh grafik
//            invalidate()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}