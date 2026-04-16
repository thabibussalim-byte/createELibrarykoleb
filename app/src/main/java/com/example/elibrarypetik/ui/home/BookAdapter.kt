package com.example.elibrarypetik.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.elibrarypetik.data.api.model.AuthorItem
import com.example.elibrarypetik.data.api.model.BookItem
import com.example.elibrarypetik.databinding.ItemBukuBinding

class BookAdapter(
    private var listBook: List<BookItem>,
    private var listAuthor: List<AuthorItem> = emptyList(), // Menampung daftar penulis
    private val onItemClick: (BookItem) -> Unit = {} // Tambahkan listener klik
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
        
        // Cari nama penulis berdasarkan id
        val authorName = listAuthor.find { it.id == book.penulisId }?.namaPenulis ?: "Memuat..."

        holder.binding.apply {
            tvJudul.text = book.judulBuku
            tvPenulis.text = authorName // Menampilkan Nama Penulis
            ratingBar.rating = 4.5f
            
            Glide.with(holder.itemView.context)
                .load(book.foto)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(ivCover)

            root.setOnClickListener {
                onItemClick(book)
            }
        }
    }

    override fun getItemCount(): Int = listBook.size
}