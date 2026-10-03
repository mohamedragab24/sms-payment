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
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: return
        val providerName = intent.getStringExtra(EXTRA_PROVIDER_NAME) ?: ""

        supportActionBar?.title = providerName
        binding.tvHeader.text = providerName

        adapter = MessageAdapter(emptyList())
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).messageDao()
            val messages = dao.getMessagesForProvider(providerId)
            adapter.updateItems(messages)
            binding.tvEmptyState.visibility =
                if (messages.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
