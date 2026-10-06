package com.example.smstotelegram

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smstotelegram.databinding.ActivityMainBinding
import com.example.smstotelegram.ui.HomeFragment
import com.example.smstotelegram.ui.MessagesFragment
import com.example.smstotelegram.ui.SettingsFragment

/**
 * الشاشة المضيفة الرئيسية، فيها شريط تنقل سفلي بثلاث صفحات:
 * الرئيسية (إحصائيات) - الرسائل (مزودي الخدمة) - الإعدادات (ربط تليجرام)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
            // فحص التحديثات تلقائيًا عند فتح التطبيق
            AppUpdater.checkAndPrompt(this, false)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_messages -> MessagesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
