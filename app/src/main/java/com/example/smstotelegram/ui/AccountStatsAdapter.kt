package com.example.smstotelegram.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smstotelegram.databinding.ItemAccountStatBinding
import com.example.smstotelegram.db.AccountStat

class AccountStatsAdapter(private var items: List<AccountStat>, private val onClick: ((AccountStat) -> Unit)? = null) : RecyclerView.Adapter<AccountStatsAdapter.VH>() {
    inner class VH(val binding: ItemAccountStatBinding): RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        ItemAccountStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvAccountNumber.text = item.recipientNumber
        holder.binding.tvProvider.text = item.providerName
        holder.binding.tvTotal.text = format(item.totalAmount) + " عملة مستلمة"
        holder.binding.tvTransactions.text = item.transactionCount.toString() + " عملية"
        holder.binding.root.setOnClickListener { onClick?.invoke(item) }
    }
    override fun getItemCount() = items.size
    fun updateItems(value: List<AccountStat>) { items = value; notifyDataSetChanged() }
    private fun format(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.2f", v)
}
