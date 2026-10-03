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
            val counts = dao.getCountsPerProvider().associateBy { it.providerId }

            val items = providers.map { provider ->
                ProviderUiItem(provider, counts[provider.id]?.count ?: 0)
            }
            adapter.updateItems(items)
            binding.tvEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddProviderDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(context).apply { hint = "اسم مزود الخدمة (مثال: فودافون كاش)" }
        val senderInput = EditText(context).apply { hint = "رقم أو اسم المرسل" }
        layout.addView(nameInput)
        layout.addView(senderInput)

        AlertDialog.Builder(context)
            .setTitle("إضافة مزود خدمة جديد")
            .setView(layout)
            .setPositiveButton("إضافة") { _, _ ->
                val name = nameInput.text.toString().trim()
                val sender = senderInput.text.toString().trim()
                if (name.isBlank() || sender.isBlank()) {
                    Toast.makeText(context, "لازم تملأ الحقلين", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Prefs.addProvider(context, Provider(name = name, senderPattern = sender))
                loadProviders()
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
