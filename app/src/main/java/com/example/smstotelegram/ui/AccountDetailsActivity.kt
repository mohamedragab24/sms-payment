package com.example.smstotelegram.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smstotelegram.databinding.ActivityAccountDetailsBinding
import com.example.smstotelegram.db.AppDatabase
import com.example.smstotelegram.db.MessageEntity
import kotlinx.coroutines.launch

class AccountDetailsActivity : AppCompatActivity() {
    companion object { const val EXTRA_PROVIDER_ID="provider_id"; const val EXTRA_ACCOUNT="account" }
    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var adapter: MessageAdapter
    private var all = emptyList<MessageEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityAccountDetailsBinding.inflate(layoutInflater); setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title="تفاصيل الحساب"
        val providerId=intent.getStringExtra(EXTRA_PROVIDER_ID) ?: return
        val account=intent.getStringExtra(EXTRA_ACCOUNT) ?: return
        binding.tvAccount.text=account
        adapter=MessageAdapter(emptyList()); binding.rvMessages.layoutManager=LinearLayoutManager(this); binding.rvMessages.adapter=adapter
        binding.etSearch.setOnQueryTextListener(object: android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q:String?)=true
            override fun onQueryTextChange(q:String?):Boolean { filter(q.orEmpty()); return true }
        })
        lifecycleScope.launch {
            val dao=AppDatabase.getInstance(applicationContext).messageDao()
            val stats=dao.getAccountStatsForProvider(providerId).firstOrNull{it.recipientNumber==account}
            binding.tvProvider.text=stats?.providerName ?: ""
            binding.tvAmount.text=format(stats?.totalAmount ?: 0.0)
            binding.tvTransactions.text=(stats?.transactionCount ?: 0).toString()
            all=dao.getMessagesForAccount(providerId, account)
            filter("")
        }
    }
    private fun filter(q:String){ val s=q.trim().lowercase(); adapter.updateItems(if(s.isBlank()) all else all.filter{it.body.lowercase().contains(s)||it.sender.lowercase().contains(s)||it.recipientNumber.contains(s)}) }
    private fun format(v:Double)=if(v%1.0==0.0)v.toLong().toString() else String.format("%.2f",v)
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}
