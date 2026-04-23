package com.example.petbook.ui.history

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petbook.R
import com.example.petbook.databinding.FragmentSuccessReturnBinding

class SuccessReturnFragment : Fragment() {

    private var _binding: FragmentSuccessReturnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuccessReturnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val bookTitle = arguments?.getString("book_title") ?: "Buku"
        

        val fullText = "Buku $bookTitle berhasil dikembalikan. Teruslah membaca untuk memperluas wawasan!"
        val spannable = SpannableStringBuilder(fullText)
        
        val startIndex = fullText.indexOf(bookTitle)
        val endIndex = startIndex + bookTitle.length
        
        if (startIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.accent_blue)),
                startIndex,
                endIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        binding.tvSuccessMessage.text = spannable


        binding.btnToCatalog.setOnClickListener {
            findNavController().navigate(R.id.bookFragment)
        }


        binding.btnBackToHome.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
