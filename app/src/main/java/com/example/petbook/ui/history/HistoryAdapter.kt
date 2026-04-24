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

        val transactionFines = listFines.filter { it.transaksiId == history.id }
        val totalDendaBuku = transactionFines.sumOf { it.totalDenda.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val isLunas = transactionFines.all { it.status.lowercase() == "dibayar" } && transactionFines.isNotEmpty()

        holder.binding.apply {
            tvHistoryTitle.text = book?.judulBuku ?: "Buku tidak ditemukan"
            tvHistoryAuthor.text = authorName
            
            val tglPinjam = history.tglPinjam.take(10)
            val tglKembali = history.tglKembali.take(10)
            tvHistoryDateRange.text = "$tglPinjam s/d $tglKembali"

            val status = history.status.lowercase()
            tvHistoryStatusBadge.text = status.uppercase()
            
            when (status) {
                "pending" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#9A3412"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#F59E0B")) 
                }
                "dipinjam" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#1E40AF"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#3B82F6"))
                }
                "dikembalikan", "selesai" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dikembalikan)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#065F46"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#10B981"))
                }
                else -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                    tvHistoryStatusBadge.setTextColor(Color.parseColor("#991B1B"))
                    viewStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
                }
            }

            // Area Denda (Menampilkan total denda per buku)
            if (totalDendaBuku > 0) {
                tvHistoryFine.visibility = View.VISIBLE
                if (isLunas) {
                    tvHistoryFine.text = "Denda: Rp $totalDendaBuku (Lunas)"
                    tvHistoryFine.setTextColor(Color.parseColor("#10B981"))
                } else {
                    tvHistoryFine.text = "Denda: Rp $totalDendaBuku (Belum Bayar)"
                    tvHistoryFine.setTextColor(Color.parseColor("#EF4444"))
                }
            } else {
                tvHistoryFine.visibility = View.GONE
            }

            Glide.with(holder.itemView.context)
                .load(book?.foto)
                .placeholder(R.drawable.narasi)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(history) }
        }
    }

    override fun getItemCount(): Int = listHistory.size
}
