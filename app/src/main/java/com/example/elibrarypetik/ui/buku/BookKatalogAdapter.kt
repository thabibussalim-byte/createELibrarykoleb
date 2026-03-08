package com.example.elibrarypetik.ui.buku

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.elibrarypetik.R
import com.example.elibrarypetik.data.model.Book
import com.example.elibrarypetik.databinding.ItemBookKatalogBinding

class BookKatalogAdapter(
    private val listBook: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookKatalogAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBookKatalogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookKatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = listBook[position]
        holder.binding.apply {
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author
            
            Glide.with(holder.itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book)
                .into(ivBookCover)

            root.setOnClickListener { onItemClick(book) }
        }
    }

    override fun getItemCount(): Int = listBook.size
}