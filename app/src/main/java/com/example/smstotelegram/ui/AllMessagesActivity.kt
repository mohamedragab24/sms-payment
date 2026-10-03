package com.example.smstotelegram.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smstotelegram.Prefs
import com.example.smstotelegram.databinding.ActivityAllMessagesBinding
import com.example.smstotelegram.db.AppDatabase
import com.example.smstotelegram.db.MessageEntity
import kotlinx.coroutines.launch

class AllMessagesActivity: AppCompatActivity(){
    private lateinit var binding:ActivityAllMessagesBinding
    private lateinit var adapter:MessageAdapter
    private var all=emptyList<MessageEntity>()
    private var providerFilter="الكل"
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState); binding=ActivityAllMessagesBinding.inflate(layoutInflater); setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true); supportActionBar?.title="إجمالي عدد الرسائل"
        adapter=MessageAdapter(emptyList()); binding.rvMessages.layoutManager=LinearLayoutManager(this); binding.rvMessages.adapter=adapter
        val providers=listOf("الكل")+Prefs.getProviders(this).map{it.name}
        binding.spProvider.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,providers)
        binding.spProvider.setOnItemSelectedListener(object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p:android.widget.AdapterView<*>?){}
            override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){providerFilter=providers[pos];filter(binding.etSearch.query?.toString().orEmpty())}
        })
        binding.etSearch.setOnQueryTextListener(object:android.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(q:String?)=true
            override fun onQueryTextChange(q:String?):Boolean{filter(q.orEmpty());return true}
        })
        lifecycleScope.launch{all=AppDatabase.getInstance(applicationContext).messageDao().getAllMessages(); binding.tvCount.text=all.size.toString();filter("")}
    }
    private fun filter(q:String){val s=q.trim().lowercase();adapter.updateItems(all.filter{(providerFilter=="الكل"||it.providerName==providerFilter)&&(s.isBlank()||it.body.lowercase().contains(s)||it.sender.lowercase().contains(s)||it.recipientNumber.contains(s))})}
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}
