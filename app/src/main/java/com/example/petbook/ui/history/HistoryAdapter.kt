package com.example.petbook.ui.history

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private var listHistory: List<HistoryDataItem>,
    private var listBooks: List<BookItem>,
    private var listAuthors: List<AuthorItem>,
    private var listFines: List<FineDataItem> = emptyList(),
    private val onItemClick: (HistoryDataItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(
        newHistory: List<HistoryDataItem>,
        books: List<BookItem>,
        authors: List<AuthorItem>,
        fines: List<FineDataItem>
    ) {
        listHistory = newHistory
        listBooks = books
        listAuthors = authors
        listFines = fines
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = listHistory[position]
        
        val book = listBooks.find { it.id == history.bukuId }
        val authorName = listAuthors.find { it.id == book?.penulisId }?.namaPenulis ?: "Penulis Anonim"

        holder.binding.apply {
            tvHistoryTitle.text = book?.judulBuku ?: "Buku tidak ditemukan"
            tvHistoryAuthor.text = authorName
            
            val status = history.status.lowercase()

            when (status) {
                "pending" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#C2410C"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#F59E0B"))
                    tvHistoryStatusBadge.text = "PENDING"
                }
                "dipinjam" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#1D4ED8"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#3B82F6"))
                    tvHistoryStatusBadge.text = "DIPINJAM"
                }
                "dikembalikan", "selesai" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dikembalikan)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#047857"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#10B981"))
                    tvHistoryStatusBadge.text = "DIKEMBALIKAN"
                }
                else -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
                    tvHistoryStatusBadge.text = "TELAT"
                }
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(history)
        }
    }

    override fun getItemCount(): Int = listHistory.size
}
