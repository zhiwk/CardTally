package com.example.cardtally

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap
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

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageHelper.attachLanguageContext(newBase))
    }

    override fun onResume() {
        super.onResume()
        com.example.cardtally.analytics.LaunchAnalytics.recordLaunch(this)
    }

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navShell: View
    private lateinit var fragmentContainer: View
    private lateinit var localeTransitionOverlay: View
    private val floatingViewBaselines = WeakHashMap<View, Int>()
    private var isBottomNavigationTemporarilyHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
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
        navShell.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyFloatingNavigationInsets()
        }

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
            is SettingsFragment, is AppearanceSettingsFragment, is AboutFragment, is BottomNavigationSettingsFragment, is com.example.cardtally.cloud.CloudBackupFragment -> R.id.nav_settings
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
        refreshBottomNavigationStyle()
        fragment.view?.post { applyFloatingNavigationInsets() }
    }

    fun refreshBottomNavigationStyle() {
        val docked = com.example.cardtally.util.BottomNavigationPreferences.isDocked(this)
        val side = if (docked) 0 else (21 * resources.displayMetrics.density + 0.5f).toInt()
        val top = if (docked) 0 else (8 * resources.displayMetrics.density + 0.5f).toInt()
        val bottom = if (docked) 0 else resources.getDimensionPixelSize(R.dimen.floating_nav_shell_bottom_padding)
        navShell.setPaddingRelative(side, top, side, bottom)
        val menuParams = bottomNavigationView.layoutParams
        menuParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        bottomNavigationView.layoutParams = menuParams
        (bottomNavigationView as? com.example.cardtally.view.ElasticBottomNavigationView)?.setDocked(docked)
        val pageParams = fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        val reserve = docked && navShell.visibility == View.VISIBLE
        pageParams.bottomToTop = if (reserve) R.id.nav_shell else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
        pageParams.bottomToBottom = if (reserve) androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        fragmentContainer.layoutParams = pageParams
        navShell.post { applyFloatingNavigationInsets() }
    }

    internal fun applyFloatingNavigationInsets() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) ?: return
        val root = fragment.view ?: return
        val dockedVisible = navShell.visibility == View.VISIBLE && com.example.cardtally.util.BottomNavigationPreferences.isDocked(this)
        val inset = if (navShell.visibility == View.VISIBLE && !dockedVisible) navShell.height else 0
        // Only scrolling tails and anchored controls reserve room. The page canvas stays full-screen.
        for (id in intArrayOf(R.id.fab_add, R.id.fab_scroll_top)) {
            val fab = root.findViewById<View>(id) ?: continue
            val params = fab.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
            val baseline = floatingViewBaselines.getOrPut(fab) { params.bottomMargin }
            val target = baseline + inset
            if (params.bottomMargin != target) {
                params.bottomMargin = target
                fab.layoutParams = params
            }
        }
        if (fragment is AgentFragment) {
            root.findViewById<View>(R.id.layout_agent_main_surface)?.let { surface ->
                val baseline = floatingViewBaselines.getOrPut(surface) { surface.paddingBottom }
                val target = baseline + inset
                if (surface.paddingBottom != target) surface.setPaddingRelative(
                    surface.paddingStart, surface.paddingTop, surface.paddingEnd, target
                )
            }
            return
        }
        if (fragment !is LedgerFragment && fragment !is StatisticsFragment &&
            fragment !is AssetFragment && fragment !is SettingsFragment) return
        val tail = com.example.cardtally.util.NavigationScrollTail.resolve(
            inset, navShell.paddingTop,
            resources.getDimensionPixelSize(R.dimen.navigation_scroll_gap)
        )
        fun updateScrollTail(view: View) {
            val pageScroll = when (fragment) {
                is LedgerFragment -> view is androidx.recyclerview.widget.RecyclerView && view.id == R.id.recycler_records
                is StatisticsFragment -> view is androidx.core.widget.NestedScrollView && view.id == R.id.scroll_content
                is AssetFragment -> view is androidx.core.widget.NestedScrollView && view.id == R.id.asset_content_scroll
                is SettingsFragment -> view === root && view is android.widget.ScrollView
                else -> false
            }
            if (pageScroll) {
                // Only whole-page containers: never pad day lists, chart pages or asset groups.
                if (view.paddingBottom != tail) view.setPaddingRelative(
                    view.paddingStart, view.paddingTop, view.paddingEnd, tail
                )
            }
            if (view is ViewGroup) for (index in 0 until view.childCount) updateScrollTail(view.getChildAt(index))
        }
        updateScrollTail(root)
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
