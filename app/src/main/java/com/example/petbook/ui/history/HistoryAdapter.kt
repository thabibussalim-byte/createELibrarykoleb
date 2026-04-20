package com.example.petbook.ui.history

import android.annotation.SuppressLint
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
        val fine = listFines.find { it.transaksiId == history.id }

        holder.binding.apply {
            tvHistoryTitle.text = book?.judulBuku ?: "Buku tidak ditemukan"
            tvHistoryAuthor.text = authorName
            
            val tglPinjam = history.tglPinjam.take(10)
            val tglKembali = history.tglKembali.take(10)
            tvHistoryDateRange.text = "$tglPinjam s/d $tglKembali"

            // Setup Status Badge
            val status = history.status.lowercase()
            tvHistoryStatusBadge.text = status.uppercase()
            when (status) {
                "pending" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                "dipinjam" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                "dikembalikan", "selesai" -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_aktif)
                else -> tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
            }

            // LOGIKA BARU: Denda hanya muncul jika status sudah DIKEMBALIKAN atau SELESAI
            val isReturned = status == "dikembalikan" || status == "selesai"
            val dendaAmount = fine?.totalDenda?.toIntOrNull() ?: 0

            if (isReturned && dendaAmount > 0) {
                tvHistoryFine.visibility = View.VISIBLE
                val statusBayar = if (fine?.status == "dibayar") "(Lunas)" else "(Belum Bayar)"
                tvHistoryFine.text = "Denda: Rp $dendaAmount $statusBayar"
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