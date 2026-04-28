package com.example.petbook.ui.buku

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petbook.R
import com.example.petbook.data.api.model.AuthorItem
import com.example.petbook.data.api.model.BookItem
import com.example.petbook.databinding.ItemBookKatalogBinding

class BookKatalogAdapter(
    private var listBook: List<BookItem>,
    private var listAuthor: List<AuthorItem> = emptyList(), // Tambahkan parameter daftar penulis
    private val onItemClick: (BookItem, Float) -> Unit // Tambahkan parameter rating
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

    fun getAuthorName(authorId: Int): String {
        return listAuthor.find { it.id == authorId }?.namaPenulis ?: "Penulis Anonim"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookKatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        val authorName = getAuthorName(book.penulisId)

        holder.binding.apply {
            tvBookTitle.text = book.judulBuku
            tvBookAuthor.text = authorName 
            
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