package com.example.petbook.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.data.model.HistoryItem
import com.example.petbook.databinding.FragmentDetailDendaBinding
import com.example.petbook.ui.history.HistoryAdapter

class DetailDendaFragment : Fragment() {

    private var _binding: FragmentDetailDendaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailDendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDendaList()
    }

    private fun setupDendaList() {



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}