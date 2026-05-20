package com.Taybetibrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var history: List<com.Taybetibrowser.data.DatabaseHelper.HistoryEntry>,
    private val onClick: (com.Taybetibrowser.data.DatabaseHelper.HistoryEntry) -> Unit,
    private val onDelete: (com.Taybetibrowser.data.DatabaseHelper.HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.history_title)
        val url: TextView = view.findViewById(R.id.history_url)
        val time: TextView = view.findViewById(R.id.history_time)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = history[position]
        holder.title.text = entry.title.ifEmpty { entry.url }
        holder.url.text = entry.url
        holder.time.text = formatTime(entry.visitedAt)
        holder.itemView.setOnClickListener { onClick(entry) }
        holder.btnDelete.setOnClickListener { onDelete(entry) }
    }

    override fun getItemCount() = history.size

    fun updateData(newHistory: List<com.Taybetibrowser.data.DatabaseHelper.HistoryEntry>) {
        history = newHistory
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }
}