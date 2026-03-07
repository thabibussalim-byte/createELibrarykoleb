package com.example.elibrarypetik.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.elibrarypetik.data.model.Book
import com.example.elibrarypetik.databinding.ItemBukuBinding

class BookAdapter(private val listBook: List<Book>) : RecyclerView.Adapter<BookAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBukuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBukuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        holder.binding.apply {
            tvJudul.text = book.title
            tvPenulis.text = book.author
            ratingBar.rating = book.rating
            
            // Perbaikan pemuatan gambar dengan Glide
            Glide.with(holder.itemView.context)
                .load(book.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Simpan cache agar lebih cepat
                .centerCrop() // Pastikan gambar memenuhi area
                .into(ivCover)
        }
    }

    override fun getItemCount(): Int = listBook.size
}