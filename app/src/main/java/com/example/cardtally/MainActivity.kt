package com.example.cardtally

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.RecurringRecordScheduler
import com.example.cardtally.util.ThemeHelper
import com.example.cardtally.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navShell: View
    private lateinit var fragmentContainer: View
    private lateinit var localeTransitionOverlay: View
    private var isBottomNavigationTemporarilyHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageHelper.applySavedLanguage(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        // AppCompat may reapply the base theme when resolving night mode during super.onCreate.
        // Apply the current opacity afterwards, before any views (including restored fragments) inflate.
        applyCardOpacity()
        setContentView(R.layout.activity_main)
        if (ThemeHelper.getTheme(this) == ThemeHelper.THEME_WALLPAPER) {
            com.example.cardtally.util.WallpaperHelper.bindImage(findViewById(R.id.image_wallpaper))
            findViewById<View>(R.id.wallpaper_background).visibility = View.VISIBLE
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        navShell = findViewById(R.id.nav_shell)
        fragmentContainer = findViewById(R.id.fragment_container)
        localeTransitionOverlay = findViewById(R.id.locale_transition_overlay)

        updateBottomNavigationVisibility()
        handlePendingLocaleTransition()

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                if (f.id == R.id.fragment_container) {
                    updateBottomNavigationForFragment(f)
                }
            }
        }, false)

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
                R.id.nav_ledger -> LedgerFragment()
                R.id.nav_asset -> AssetFragment()
                R.id.nav_statistics -> StatisticsFragment()
                R.id.nav_agent -> AgentFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }

            selectedFragment?.let {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment != null && currentFragment::class == it::class) {
                    return@setOnItemSelectedListener true
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }

        bottomNavigationView.setOnItemReselectedListener { }

        val restoredDestination = when (supportFragmentManager.findFragmentById(R.id.fragment_container)) {
            is AssetFragment -> R.id.nav_asset
            is StatisticsFragment -> R.id.nav_statistics
            is AgentFragment -> R.id.nav_agent
            is SettingsFragment, is AppearanceSettingsFragment -> R.id.nav_settings
            else -> R.id.nav_ledger
        }
        bottomNavigationView.menu.findItem(restoredDestination).isChecked = true

        if (savedInstanceState == null) {
            val initialFragment = if (QuickAddHelper.getQuickAdd(this)) {
                AddRecordFragment()
            } else {
                LedgerFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit()
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                updateBottomNavigationForFragment(it)
            }
        }
    }

    private fun applyTheme() {
        val mode = ThemeHelper.getTheme(this)
        ThemeHelper.applyThemeMode(mode, ThemeHelper.getWallpaperPalette(this))
        setTheme(ThemeHelper.getThemeResId(mode))
    }

    private fun applyCardOpacity() {
        if (ThemeHelper.getTheme(this) == ThemeHelper.THEME_WALLPAPER) {
            theme.applyStyle(ThemeHelper.getCardOpacityOverlayResId(ThemeHelper.getCardOpacity(this)), true)
        }
    }

    override fun onStart() {
        super.onStart()
        DatabaseHelper(this).also {
            it.processDueRecurringRecordsForAllLedgers()
            it.close()
        }
        RecurringRecordScheduler.schedule(this)
    }

    private fun handlePendingLocaleTransition() {
        if (!LanguageHelper.consumePendingLocaleTransition(this)) {
            localeTransitionOverlay.visibility = View.GONE
            return
        }

        localeTransitionOverlay.alpha = 1f
        localeTransitionOverlay.visibility = View.VISIBLE

        fragmentContainer.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (fragmentContainer.viewTreeObserver.isAlive) {
                    fragmentContainer.viewTreeObserver.removeOnPreDrawListener(this)
                }
                fragmentContainer.post {
                    localeTransitionOverlay.animate()
                        .alpha(0f)
                        .setDuration(120)
                        .withEndAction {
                            localeTransitionOverlay.visibility = View.GONE
                            localeTransitionOverlay.alpha = 1f
                        }
                        .start()
                }
                return true
            }
        })
    }

    private fun updateBottomNavigationVisibility() {
        val showAiAssistant = AiAssistantSettingsHelper.getAiAssistantEnabled(this)
        val showAsset = AssetDisplayHelper.getShowAsset(this)
        val menu = bottomNavigationView.menu
        val aiItem = menu.findItem(R.id.nav_agent)
        val assetItem = menu.findItem(R.id.nav_asset)
        aiItem.isVisible = showAiAssistant
        assetItem.isVisible = showAsset
    }

    private fun updateBottomNavigationForFragment(fragment: Fragment) {
        val isTopLevel = fragment is LedgerFragment ||
            fragment is AssetFragment ||
            fragment is StatisticsFragment ||
            fragment is AgentFragment ||
            fragment is SettingsFragment

        val visibility = if (isTopLevel && !isBottomNavigationTemporarilyHidden) View.VISIBLE else View.GONE
        navShell.visibility = visibility
        bottomNavigationView.visibility = visibility
    }

    fun setBottomNavigationTemporarilyHidden(hidden: Boolean) {
        if (isBottomNavigationTemporarilyHidden == hidden) {
            return
        }

        isBottomNavigationTemporarilyHidden = hidden
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
            updateBottomNavigationForFragment(it)
        }
    }
}
