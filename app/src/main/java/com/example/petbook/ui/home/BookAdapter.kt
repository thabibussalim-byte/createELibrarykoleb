package com.example.petbook.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.databinding.ItemBukuBinding

class BookAdapter(
    private var listBook: List<BookItem>,
    private var listAuthor: List<AuthorItem> = emptyList(),
    private val onItemClick: (BookItem, Float) -> Unit = { _, _ -> } // Tambahkan parameter rating
) : RecyclerView.Adapter<BookAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBukuBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<BookItem>) {
        listBook = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAuthors(authors: List<AuthorItem>) {
        listAuthor = authors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBukuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        val authorName = listAuthor.find { it.id == book.penulisId }?.namaPenulis ?: "Memuat..."

        holder.binding.apply {
            tvJudul.text = book.judulBuku
            tvPenulis.text = authorName 
            
            // Gunakan rumus yang sama agar konsisten
            val dummyRating = (38 + (book.id % 12)).toFloat() / 10
            ratingBar.rating = dummyRating
            
            Glide.with(holder.itemView.context)
                .load(book.foto)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(ivCover)

            root.setOnClickListener {
                onItemClick(book, dummyRating) // Kirim rating saat diklik
            }
        }
    }

    override fun getItemCount(): Int = listBook.size
}