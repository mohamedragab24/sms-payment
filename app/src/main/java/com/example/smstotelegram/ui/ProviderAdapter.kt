package com.example.smstotelegram.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smstotelegram.Provider
import com.example.smstotelegram.databinding.ItemProviderBinding

data class ProviderUiItem(
    val provider: Provider,
    val accountCount: Int,
    val totalAmount: Double,
    val transactionCount: Int
)

class ProviderAdapter(
    private var items: List<ProviderUiItem>,
    private val onClick: (Provider) -> Unit,
    private val onLongClick: (Provider) -> Unit
) : RecyclerView.Adapter<ProviderAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProviderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProviderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvProviderName.text = item.provider.name
        holder.binding.tvProviderPattern.text = "المستلم: ${item.provider.recipientNumber}"
        holder.binding.tvSenderId.text = "المرسل: ${item.provider.senderId ?: "غير محدد"}"
        holder.binding.tvMessageCount.text = "${item.accountCount} حساب\n${formatAmount(item.totalAmount)} عملة\n${item.transactionCount} عملية"
        holder.binding.root.setOnClickListener { onClick(item.provider) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(item.provider)
            true
        }
    }

    private fun formatAmount(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.2f", v)

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ProviderUiItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
