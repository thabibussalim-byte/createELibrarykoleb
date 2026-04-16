package com.example.elibrarypetik.ui.buku

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.api.model.AuthorItem
import com.example.elibrarypetik.data.api.model.BookItem
import com.example.elibrarypetik.databinding.ItemBookKatalogBinding

class BookKatalogAdapter(
    private var listBook: List<BookItem>,
    private var listAuthor: List<AuthorItem> = emptyList(), // Tambahkan parameter daftar penulis
    private val onItemClick: (BookItem) -> Unit
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookKatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        
        // Cari nama penulis berdasarkan id dari daftar penulis
        val authorName = listAuthor.find { it.id == book.penulisId }?.namaPenulis ?: "Penulis Anonim"

        holder.binding.apply {
            tvBookTitle.text = book.judulBuku
            tvBookAuthor.text = authorName // Ganti Stok menjadi Nama Penulis
            
            Glide.with(holder.itemView.context)
                .load(book.foto)
                .placeholder(R.drawable.ic_book)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(book) }
        }
    }

    override fun getItemCount(): Int = listBook.size
}