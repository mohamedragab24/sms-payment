package com.example.smstotelegram.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smstotelegram.databinding.ItemMessageBinding
import com.example.smstotelegram.db.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private var items: List<MessageEntity>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSender.text = item.sender
        holder.binding.tvBody.text = item.body
        holder.binding.tvTimestamp.text = dateFormat.format(Date(item.timestamp))
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MessageEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
