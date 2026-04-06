package com.example.elibrarypetik.ui.bantuan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.elibrarypetik.databinding.FragmentFaqDetailBinding

class FaqDetailFragment : Fragment() {

    private var _binding: FragmentFaqDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data yang dikirim dari HelpFragment
        val title = arguments?.getString("faq_title")
        val content = arguments?.getString("faq_answer")

        // Tampilkan ke UI
        binding.tvFaqTitle.text = title
        binding.tvFaqContent.text = content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}