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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: return
        val providerName = intent.getStringExtra(EXTRA_PROVIDER_NAME) ?: ""
        supportActionBar?.title = providerName
        binding.tvHeader.text = "حسابات $providerName"
        adapter = AccountStatsAdapter(emptyList())
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
        lifecycleScope.launch {
            val items = AppDatabase.getInstance(applicationContext).messageDao().getAccountStatsForProvider(providerId)
            adapter.updateItems(items)
            binding.tvEmptyState.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
