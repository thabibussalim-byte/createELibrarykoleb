package com.example.petbook.ui.buku

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.data.api.model.GenreItem
import com.example.petbook.databinding.ItemBookKatalogBinding

class BookKatalogAdapter(
    private var listBook: List<BookItem>,
    private var listAuthor: List<AuthorItem> = emptyList(),
    private var listGenre: List<GenreItem> = emptyList(),
    private val onItemClick: (BookItem, Float) -> Unit
) : RecyclerView.Adapter<BookKatalogAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBookKatalogBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<BookItem>) {
        listBook = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAuthors(authors: List<AuthorItem>) {
        listAuthor = authors
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGenres(genres: List<GenreItem>) {
        listGenre = genres
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookKatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        val authorName = listAuthor.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"
        val genre = listGenre.find { it.id == book.genreId }
        val genreName = genre?.namaGenre ?: "Umum"

        holder.binding.apply {
            tvBookTitle.text = book.judulBuku
            tvBookAuthor.text = authorName 
            tvBookGenreLabel.text = genreName

            // --- LOGIKA WARNA DINAMIS UNTUK 12 GENRE ---
            val (indicatorColor, badgeBgColor, badgeTextColor) = when (genreName.lowercase()) {
                "teknologi" -> Triple("#3B82F6", "#DBEAFE", "#1E40AF") // Biru
                "agama" -> Triple("#10B981", "#D1FAE5", "#065F46")    // Hijau
                "kesehatan" -> Triple("#EF4444", "#FEE2E2", "#991B1B") // Merah
                "pendidikan" -> Triple("#F59E0B", "#FFEDD5", "#9A3412") // Orange
                "pengembangan diri", "self improvement", "psikologi" -> Triple("#8B5CF6", "#EDE9FE", "#5B21B6") // Ungu
                "sains" -> Triple("#06B6D4", "#CFFAFE", "#164E63") // Teal/Cyan
                "bisnis", "marketing" -> Triple("#6366F1", "#E0E7FF", "#3730A3") // Indigo
                "sejarah", "biografi" -> Triple("#D97706", "#FEF3C7", "#78350F") // Amber/Cokelat
                else -> Triple("#94A3B8", "#F1F5F9", "#475569") // Abu-abu
            }

            // 1. Set Warna Garis Indikator (Sisi Kiri)
            viewGenreIndicator.setBackgroundColor(Color.parseColor(indicatorColor))

            // 2. Set Warna Badge Genre (Background Pill & Text)
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor(badgeBgColor))
            }
            tvBookGenreLabel.background = shape
            tvBookGenreLabel.setTextColor(Color.parseColor(badgeTextColor))

            val dummyRating = (38 + (book.id % 12)).toFloat() / 10
            
            Glide.with(holder.itemView.context)
                .load(book.foto)
                .placeholder(R.drawable.ic_book)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(book, dummyRating) }
        }
    }

    override fun getItemCount(): Int = listBook.size
}
