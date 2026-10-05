package com.example.smstotelegram.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smstotelegram.Prefs
import com.example.smstotelegram.Provider
import com.example.smstotelegram.databinding.FragmentMessagesBinding
import com.example.smstotelegram.db.AppDatabase
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProviderAdapter
    private var allItems = emptyList<ProviderUiItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProviderAdapter(
            items = emptyList(),
            onClick = { provider ->
                val intent = android.content.Intent(
                    requireContext(), ProviderMessagesActivity::class.java
                )
                intent.putExtra(ProviderMessagesActivity.EXTRA_PROVIDER_ID, provider.id)
                intent.putExtra(ProviderMessagesActivity.EXTRA_PROVIDER_NAME, provider.name)
                startActivity(intent)
            },
            onLongClick = { provider -> confirmDelete(provider) }
        )
        binding.rvProviders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProviders.adapter = adapter

        binding.fabAddProvider.setOnClickListener { showAddProviderDialog() }
        binding.etSearch.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = true
            override fun onQueryTextChange(q: String?): Boolean {
                filterItems(q.orEmpty()); return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadProviders()
    }

    private fun loadProviders() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val providers = Prefs.getProviders(context)
            val dao = AppDatabase.getInstance(context).messageDao()
            val stats = dao.getProviderStats().associateBy { it.providerId }
            val items = providers.map { provider ->
                val st = stats[provider.id]
                ProviderUiItem(provider, st?.accountCount ?: 0, st?.totalAmount ?: 0.0, st?.transactionCount ?: 0)
            }
            allItems = items
            filterItems(binding.etSearch.query?.toString().orEmpty())
            binding.tvEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }


    private fun filterItems(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) allItems else allItems.filter {
            it.provider.name.lowercase().contains(q) || it.provider.recipientNumber.contains(q) || (it.provider.senderId ?: "").lowercase().contains(q)
        }
        adapter.updateItems(filtered)
    }

    private fun showAddProviderDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(context).apply { hint = "اسم مزود الخدمة (مثال: فودافون كاش)" }
        val recipientInput = EditText(context).apply { hint = "رقم المستلم / رقم الحساب"; inputType = android.text.InputType.TYPE_CLASS_PHONE }
        val senderInput = EditText(context).apply { hint = "اسم المرسل المسموح فقط (مثال: VF-Cash)" }
        layout.addView(nameInput)
        layout.addView(senderInput)
        layout.addView(recipientInput)

        AlertDialog.Builder(context)
            .setTitle("إضافة مزود خدمة جديد")
            .setView(layout)
            .setPositiveButton("إضافة") { _, _ ->
                val name = nameInput.text.toString().trim()
                val recipient = recipientInput.text.toString().trim()
                val sender = senderInput.text.toString().trim()
                if (name.isBlank() || recipient.isBlank() || sender.isBlank()) {
                    Toast.makeText(context, "لازم تملأ اسم المزود ورقم المستلم واسم المرسل المسموح", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val provider = Provider(name = name, recipientNumber = recipient, senderId = sender)
                Prefs.addProvider(context, provider)
                loadProviders()
                viewLifecycleOwner.lifecycleScope.launch {
                    val n = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.smstotelegram.InboxImporter.import(context, provider)
                    }
                    if (n > 0) Toast.makeText(context, "تم استيراد $n رسالة قديمة", Toast.LENGTH_SHORT).show()
                    loadProviders()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDelete(provider: Provider) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف مزود الخدمة")
            .setMessage("هل تريد حذف \"${provider.name}\"؟ (الرسائل القديمة هتفضل محفوظة)")
            .setPositiveButton("حذف") { _, _ ->
                Prefs.deleteProvider(requireContext(), provider.id)
                loadProviders()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
