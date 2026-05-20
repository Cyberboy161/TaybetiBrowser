package com.Taybetibrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TabAdapter(
    private var tabs: List<MainActivity.Tab>,
    private val currentTabId: Int,
    private val onClick: (MainActivity.Tab) -> Unit,
    private val onClose: (MainActivity.Tab) -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val btnClose: ImageView = view.findViewById(R.id.tab_close)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { "New Tab" }
        holder.url.text = tab.url.replace("https://", "").replace("http://", "")

        if (tab.id == currentTabId) {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.surface_variant))
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.surface))
        }

        holder.itemView.setOnClickListener { onClick(tab) }
        holder.btnClose.setOnClickListener { onClose(tab) }
    }

    override fun getItemCount() = tabs.size

    fun updateData(newTabs: List<MainActivity.Tab>, newCurrentTabId: Int) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}