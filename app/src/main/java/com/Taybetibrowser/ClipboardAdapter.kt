package com.Taybetibrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardAdapter(
    private var entries: List<com.Taybetibrowser.data.InternalClipboard.ClipboardEntry>,
    private val onCopy: (com.Taybetibrowser.data.InternalClipboard.ClipboardEntry) -> Unit,
    private val onDelete: (com.Taybetibrowser.data.InternalClipboard.ClipboardEntry) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.clipboard_text)
        val time: TextView = view.findViewById(R.id.clipboard_time)
        val btnCopy: ImageView = view.findViewById(R.id.clipboard_copy)
        val btnDelete: ImageView = view.findViewById(R.id.clipboard_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.text.text = entry.text
        holder.time.text = formatTime(entry.timestamp)
        holder.btnCopy.setOnClickListener { onCopy(entry) }
        holder.btnDelete.setOnClickListener { onDelete(entry) }
        holder.itemView.setOnClickListener { onCopy(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateData(newEntries: List<com.Taybetibrowser.data.InternalClipboard.ClipboardEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }
}