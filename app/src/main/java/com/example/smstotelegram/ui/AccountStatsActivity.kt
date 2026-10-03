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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "إحصائيات الحسابات"
        adapter = AccountStatsAdapter(emptyList())
        binding.rvAccounts.layoutManager = LinearLayoutManager(this)
        binding.rvAccounts.adapter = adapter
        lifecycleScope.launch {
            val items = AppDatabase.getInstance(applicationContext).messageDao().getAccountStats()
            adapter.updateItems(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
