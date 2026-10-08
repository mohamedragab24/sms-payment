package com.example.smstotelegram.ui

import android.app.AlertDialog
import android.view.LayoutInflater
import com.example.smstotelegram.PaymentStatusChecker
import com.example.smstotelegram.SmsParser
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
        holder.binding.btnCheck.setOnClickListener { v ->
            val ctx = v.context
            val txId = SmsParser.findTransactionId(item.body).orEmpty()
            holder.binding.btnCheck.isEnabled = false
            PaymentStatusChecker.check(ctx, txId, item.providerName) { _, label, detail ->
                v.post {
                    holder.binding.btnCheck.isEnabled = true
                    AlertDialog.Builder(ctx)
                        .setTitle("حالة العملية: $label")
                        .setMessage(if (detail.isBlank()) "رقم العملية: ${txId.ifBlank { "غير متوفر" }}" else detail)
                        .setPositiveButton("حسنًا", null)
                        .show()
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MessageEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
