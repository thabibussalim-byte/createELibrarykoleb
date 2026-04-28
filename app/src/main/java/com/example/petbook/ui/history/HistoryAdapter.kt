package com.example.petbook.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.FineDataItem
import com.example.petbook.data.api.model.HistoryDataItem
import com.example.petbook.databinding.ItemHistoryBinding
import androidx.core.graphics.toColorInt

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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = listHistory[position]
        
        val book = listBooks.find { it.id == history.bukuId }
        val authorName = listAuthors.find { it.id == book?.penulisId }?.namaPenulis ?: "Penulis Anonim"


        holder.binding.apply {
            tvHistoryTitle.text = book?.judulBuku ?: "Buku tidak ditemukan"
            tvHistoryAuthor.text = authorName
            val ivHistoryBook = holder.binding.ivBookCover

            if (book?.foto != null) {
                Glide.with(holder.itemView.context)
                    .load(book.foto)
                    .placeholder(R.drawable.bintang)
                    .error(R.drawable.bintang)
                    .into(ivHistoryBook)
            }
            
            val status = history.status.lowercase()

            when (status) {
                "pending" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    tvHistoryStatusBadge.setTextColor("#C2410C".toColorInt())
                    viewStatusIndicator.setBackgroundColor("#F59E0B".toColorInt())
                    tvHistoryStatusBadge.text = "PENDING"
                }
                "dipinjam" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dipinjam)
                    tvHistoryStatusBadge.setTextColor("#1D4ED8".toColorInt())
                    viewStatusIndicator.setBackgroundColor("#3B82F6".toColorInt())
                    tvHistoryStatusBadge.text = "DIPINJAM"
                }
                "dikembalikan", "selesai" -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_dikembalikan)
                    tvHistoryStatusBadge.setTextColor("#047857".toColorInt())
                    viewStatusIndicator.setBackgroundColor("#10B981".toColorInt())
                    tvHistoryStatusBadge.text = "DIKEMBALIKAN"
                }
                else -> {
                    tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_telat)
                    tvHistoryStatusBadge.setTextColor("#B91C1C".toColorInt())
                    viewStatusIndicator.setBackgroundColor("#EF4444".toColorInt())
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
