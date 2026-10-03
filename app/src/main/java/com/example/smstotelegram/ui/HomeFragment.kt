package com.example.smstotelegram.ui

import android.os.Bundle
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
            binding.tvTotalProviders.text = totalProviders.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
