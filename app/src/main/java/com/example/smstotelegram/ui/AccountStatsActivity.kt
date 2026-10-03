package com.example.smstotelegram.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smstotelegram.databinding.ActivityAccountStatsBinding
import com.example.smstotelegram.db.AppDatabase
import kotlinx.coroutines.launch

class AccountStatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountStatsBinding
    private lateinit var adapter: AccountStatsAdapter
    private var all = emptyList<com.example.smstotelegram.db.AccountStat>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "إحصائيات الحسابات"
        adapter = AccountStatsAdapter(emptyList()) { item ->
            val i = android.content.Intent(this, AccountDetailsActivity::class.java)
            i.putExtra(AccountDetailsActivity.EXTRA_PROVIDER_ID, item.providerId)
            i.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item.recipientNumber)
            startActivity(i)
        }
        binding.rvAccounts.layoutManager = LinearLayoutManager(this)
        binding.rvAccounts.adapter = adapter
        lifecycleScope.launch {
            all = AppDatabase.getInstance(applicationContext).messageDao().getAccountStats()
            filter(binding.etSearch.query?.toString().orEmpty())
            binding.tvEmpty.visibility = if (all.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
        binding.etSearch.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = true
            override fun onQueryTextChange(q: String?): Boolean { filter(q.orEmpty()); return true }
        })
    }
    private fun filter(q: String) {
        val s=q.trim().lowercase()
        adapter.updateItems(if(s.isBlank()) all else all.filter{it.recipientNumber.contains(s)||it.providerName.lowercase().contains(s)})
    }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
