package com.example.petbook.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.TransactionsItem

import com.example.petbook.databinding.ItemHistoryBinding

class HistoryAdapter(
    private var listHistory: List<TransactionsItem>,
    private val onItemClick: (TransactionsItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    fun submitList(newList: List<TransactionsItem>) {
        listHistory = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = listHistory[position]
        holder.binding.apply {
            // Karena DataItem tidak punya judul buku langsung, kita tampilkan ID Buku atau "Buku"
            tvHistoryTitle.text = "Buku ID: ${transaction.bukuId}"
            tvHistoryAuthor.text = transaction.keterangan ?: "Tidak ada keterangan"
            tvHistoryDateRange.text = "${transaction.tglPinjam} - ${transaction.tglKembali}"
            
            val status = transaction.status ?: "Proses"
            tvHistoryStatusBadge.text = status
            
            // Logika warna badge berdasarkan status
            when (status.lowercase()) {
                "dipinjam" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                "terlambat" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                "selesai" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                "proses" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                "ditolak" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_ditolak)
                else -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
            }

            // Denda (jika ada field denda di API, sesuaikan. Di model DataItem belum ada)
            tvHistoryFine.visibility = View.GONE

            Glide.with(holder.itemView.context)
                .load(R.drawable.ic_home) // Placeholder karena URL gambar tidak ada di DataItem transaksi
                .placeholder(R.drawable.ic_home)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(transaction) }
        }
    }

    override fun getItemCount(): Int = listHistory.size
}