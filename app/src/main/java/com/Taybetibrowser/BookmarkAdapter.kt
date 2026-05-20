package com.Taybetibrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private var bookmarks: List<com.Taybetibrowser.data.DatabaseHelper.Bookmark>,
    private val onClick: (com.Taybetibrowser.data.DatabaseHelper.Bookmark) -> Unit,
    private val onDelete: (com.Taybetibrowser.data.DatabaseHelper.Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.bookmark_title)
        val url: TextView = view.findViewById(R.id.bookmark_url)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.title.text = bookmark.title
        holder.url.text = bookmark.url
        holder.itemView.setOnClickListener { onClick(bookmark) }
        holder.btnDelete.setOnClickListener { onDelete(bookmark) }
    }

    override fun getItemCount() = bookmarks.size

    fun updateData(newBookmarks: List<com.Taybetibrowser.data.DatabaseHelper.Bookmark>) {
        bookmarks = newBookmarks
        notifyDataSetChanged()
    }
}