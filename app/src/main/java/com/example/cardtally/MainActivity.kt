package com.example.cardtally

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageHelper.applySavedLanguage(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        
        updateAssetVisibility()
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_asset -> AssetFragment()
                R.id.nav_statistics -> StatisticsFragment()
                R.id.nav_agent -> AgentFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }

        if (savedInstanceState == null) {
            val initialFragment = if (QuickAddHelper.getQuickAdd(this)) {
                AddRecordFragment()
            } else {
                HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit()
        }
    }

    private fun applyTheme() {
        val themeMode = ThemeHelper.getTheme(this)
        val themeResId = ThemeHelper.getThemeResId(themeMode)
        setTheme(themeResId)
    }

    private fun updateAssetVisibility() {
        val showAsset = AssetDisplayHelper.getShowAsset(this)
        val menu = bottomNavigationView.menu
        val assetItem = menu.findItem(R.id.nav_asset)
        assetItem.isVisible = showAsset
    }
}
