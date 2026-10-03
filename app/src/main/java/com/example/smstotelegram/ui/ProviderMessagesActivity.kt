package com.example.smstotelegram.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smstotelegram.databinding.ActivityProviderMessagesBinding
import com.example.smstotelegram.db.AppDatabase
import kotlinx.coroutines.launch

class ProviderMessagesActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PROVIDER_ID = "provider_id"
        const val EXTRA_PROVIDER_NAME = "provider_name"
    }
    private lateinit var binding: ActivityProviderMessagesBinding
    private lateinit var adapter: AccountStatsAdapter
    private var all = emptyList<com.example.smstotelegram.db.AccountStat>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: return
        val providerName = intent.getStringExtra(EXTRA_PROVIDER_NAME) ?: ""
        supportActionBar?.title = providerName
        binding.tvHeader.text = "حسابات $providerName"
        adapter = AccountStatsAdapter(emptyList()) { item ->
            val i=android.content.Intent(this, AccountDetailsActivity::class.java)
            i.putExtra(AccountDetailsActivity.EXTRA_PROVIDER_ID, item.providerId)
            i.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item.recipientNumber)
            startActivity(i)
        }
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
        binding.etSearch.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = true
            override fun onQueryTextChange(q: String?): Boolean { filter(q.orEmpty()); return true }
        })
        lifecycleScope.launch {
            all = AppDatabase.getInstance(applicationContext).messageDao().getAccountStatsForProvider(providerId)
            filter("")
            binding.tvEmptyState.visibility = if (all.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    private fun filter(q: String) {
        val s=q.trim().lowercase()
        adapter.updateItems(if(s.isBlank()) all else all.filter{it.recipientNumber.contains(s)})
    }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
