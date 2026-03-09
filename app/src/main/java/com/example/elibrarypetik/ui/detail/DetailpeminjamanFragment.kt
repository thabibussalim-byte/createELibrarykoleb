package com.example.elibrarypetik.ui.detail

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.elibrarypetik.R
import com.example.elibrarypetik.databinding.FragmentDetailpeminjamanBinding
import java.text.SimpleDateFormat
import java.util.*

class DetailpeminjamanFragment : Fragment() {

    private var _binding: FragmentDetailpeminjamanBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailpeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDummyBookPreview()
        setupDatePicker()
        setupAutoCalculateReturnDate()

        binding.btnPinjamFinal.setOnClickListener {
            val lamaPinjam = binding.etLamaPinjam.text.toString()
            if (lamaPinjam.isNotEmpty()) {
                Toast.makeText(requireContext(), "Peminjaman berhasil diajukan!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_detailpeminjamanFragment_to_detailHistoryFragment)
            } else {
                Toast.makeText(requireContext(), "Harap isi lama peminjaman", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDummyBookPreview() {
        // Dummy data buku yang sedang diproses
        binding.tvBorrowTitle.text = "Python for Beginners"
        binding.tvBorrowAuthor.text = "Paul Deitel"
        // iv_borrow_preview sudah memiliki src default di XML, nanti bisa di-set lewat Glide
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateLabel()
        }

        binding.etTglPinjam.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel() {
        val myFormat = "dd MMM yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etTglPinjam.setText(sdf.format(calendar.time))
        calculateReturnDate()
    }

    private fun setupAutoCalculateReturnDate() {
        binding.etLamaPinjam.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateReturnDate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun calculateReturnDate() {
        val lamaPinjamStr = binding.etLamaPinjam.text.toString()
        if (lamaPinjamStr.isNotEmpty()) {
            val days = lamaPinjamStr.toInt()
            val returnCalendar = calendar.clone() as Calendar
            returnCalendar.add(Calendar.DAY_OF_YEAR, days)

            val myFormat = "dd MMM yyyy"
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            binding.tvTglKembaliEst.text = sdf.format(returnCalendar.time)
        } else {
            binding.tvTglKembaliEst.text = "-"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}