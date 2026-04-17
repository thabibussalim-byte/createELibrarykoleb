package com.example.petbook.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.model.HistoryItem
import com.example.petbook.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val listHistory: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = listHistory[position]
        holder.binding.apply {
            tvHistoryTitle.text = history.title
            tvHistoryAuthor.text = history.author
            tvHistoryDateRange.text = "${history.borrowDate} - ${history.dueDate}"
            tvHistoryStatusBadge.text = history.status
            
            // Logika warna badge berdasarkan status
            when (history.status.lowercase()) {
                "dipinjam" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                "terlambat" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                "selesai" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                "proses" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                "ditolak" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_ditolak)
                else -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
            }

            if (history.isLate && history.fine != null) {
                tvHistoryFine.visibility = View.VISIBLE
                tvHistoryFine.text = "Denda: ${history.fine}"
            } else {
                tvHistoryFine.visibility = View.GONE
            }

            Glide.with(holder.itemView.context)
                .load(history.imageUrl)
                .placeholder(R.drawable.ic_home)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(history) }
        }
    }

    override fun getItemCount(): Int = listHistory.size
}