package com.example.elibrarypetik.ui.detail

import android.app.DatePickerDialog
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
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.api.model.BookItem
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

        // Ambil data dari arguments
        val book = arguments?.let {
            BundleCompat.getParcelable(it, "book", BookItem::class.java)
        }
        val writerName = arguments?.getString("book_writer")
        val publisherName = arguments?.getString("book_publisher")

        setupBookPreview(book, writerName)
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

    private fun setupBookPreview(book: BookItem?, writerName: String?) {
        if (book != null) {
            binding.tvBorrowTitle.text = book.judulBuku
            binding.tvBorrowAuthor.text = writerName ?: "ID Penulis: ${book.penulisId}"
            
            Glide.with(this)
                .load(book.foto)
                .placeholder(R.drawable.bintang)
                .into(binding.ivBorrowPreview)
        }
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
            val days = try { lamaPinjamStr.toInt() } catch (e: Exception) { 0 }
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