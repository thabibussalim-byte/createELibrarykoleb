package com.example.petbook.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petbook.data.api.ApiConfig
import com.example.petbook.data.api.model.TransactionsResponse
import com.example.petbook.data.pref.PreferenceManager
import com.example.petbook.databinding.FragmentHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList()) { transaction ->
            Toast.makeText(requireContext(), "Klik: ${transaction.id}", Toast.LENGTH_SHORT).show()
        }
        
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        val prefManager = PreferenceManager(requireContext())
        val token = prefManager.getToken()
        val currentUserId = prefManager.getId()

        if (!token.isNullOrEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            ApiConfig.getApiService().getTransactions("Bearer $token").enqueue(object : Callback<TransactionsResponse> {
                override fun onResponse(call: Call<TransactionsResponse>, response: Response<TransactionsResponse>) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        if (response.isSuccessful) {
                            val allTransactions = response.body()?.data?.filterNotNull() ?: emptyList()
                            
                            // Filter transaksi hanya milik user yang sedang login
                            val myTransactions = allTransactions.filter { it.id == currentUserId }
                            
                            if (myTransactions.isEmpty()) {
                                binding.rvHistory.visibility = View.GONE
                            } else {
                                binding.rvHistory.visibility = View.VISIBLE
                                historyAdapter.submitList(myTransactions)
                            }
                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<TransactionsResponse>, t: Throwable) {
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        Log.e("HistoryFragment", "Error: ${t.message}")
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}