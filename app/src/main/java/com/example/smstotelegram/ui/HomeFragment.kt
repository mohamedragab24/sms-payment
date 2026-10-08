package com.example.smstotelegram.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.smstotelegram.ErrorLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smstotelegram.Prefs
import com.example.smstotelegram.databinding.FragmentHomeBinding
import com.example.smstotelegram.db.AppDatabase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        showLastError()
    }

    /** آخر خطأ/تحذير مسجّل: السبب + المطلوب للحل، مع نسخ وعرض كل الأخطاء ومسح. */
    private fun showLastError() {
        val ctx = requireContext()
        val e = ErrorLog.last(ctx)
        if (e == null) { binding.cardError.visibility = View.GONE; return }
        binding.cardError.visibility = View.VISIBLE
        binding.tvErrorTitle.text = (if (e.warning) "⚠️ " else "❌ ") + e.title
        binding.tvErrorBody.text = buildString {
            if (e.where.isNotBlank()) append("المكان: ").append(e.where).append('\n')
            if (e.cause.isNotBlank()) append("السبب: ").append(e.cause).append('\n')
            if (e.fix.isNotBlank()) append("المطلوب: ").append(e.fix)
        }.trim()
        binding.btnErrorCopy.setOnClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("error", e.toText()))
            Toast.makeText(ctx, "تم نسخ تفاصيل الخطأ", Toast.LENGTH_SHORT).show()
        }
        binding.btnErrorMore.setOnClickListener {
            val all = ErrorLog.all(ctx).joinToString("\n\n────────\n\n") { it.toText() }
            AlertDialog.Builder(ctx).setTitle("سجل الأخطاء").setMessage(all).setPositiveButton("إغلاق", null).show()
        }
        binding.btnErrorClear.setOnClickListener { ErrorLog.clear(ctx); showLastError() }
    }

    private fun loadStats() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getInstance(context).messageDao()
            val totalMessages = dao.getTotalMessagesCount()
            val totalNumbers = dao.getDistinctAccountsCount()
            val totalProviders = Prefs.getProviders(context).size

            binding.tvTotalMessages.text = totalMessages.toString()
            binding.tvTotalNumbers.text = totalNumbers.toString()
            binding.cardAccounts.setOnClickListener { startActivity(android.content.Intent(requireContext(), AccountStatsActivity::class.java)) }
            binding.cardMessages.setOnClickListener { startActivity(android.content.Intent(requireContext(), AllMessagesActivity::class.java)) }
            binding.cardProviders.setOnClickListener {
                requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.example.smstotelegram.R.id.bottomNav).selectedItemId = com.example.smstotelegram.R.id.nav_messages
            }
            binding.tvTotalProviders.text = totalProviders.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
