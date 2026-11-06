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
import com.example.cardtally.util.AiEntryStyle
import com.example.cardtally.util.AiEntryPosition
import com.example.cardtally.util.BottomNavigationPreferences
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

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !com.example.cardtally.util.LauncherIconHelper.sync(this)) {
            android.widget.Toast.makeText(this, R.string.launcher_icon_apply_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navShell: View
    private lateinit var aiNavigationButton: View
    private lateinit var fragmentContainer: View
    private lateinit var localeTransitionOverlay: View
    private val floatingViewBaselines = WeakHashMap<View, Int>()
    private var isBottomNavigationTemporarilyHidden = false
    private var pendingPrimaryDestination: Int? = null

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
        aiNavigationButton = findViewById(R.id.ai_navigation_button)
        aiNavigationButton.setOnClickListener { openPrimaryDestination(R.id.nav_agent) }
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
            openPrimaryDestination(item.itemId)
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

    private fun openPrimaryDestination(destination: Int): Boolean {
        val fragment = when (destination) {
            R.id.nav_ledger -> LedgerFragment()
            R.id.nav_asset -> AssetFragment()
            R.id.nav_statistics -> StatisticsFragment()
            R.id.nav_agent -> AgentFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> return false
        }
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (pendingPrimaryDestination == destination ||
            (pendingPrimaryDestination == null && current?.javaClass == fragment.javaClass)) return true
        pendingPrimaryDestination = destination
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .runOnCommit {
                if (pendingPrimaryDestination == destination) pendingPrimaryDestination = null
            }.commit()
        return true
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
        applyBottomNavigationOrder()
        val showAiAssistant = AiAssistantSettingsHelper.getAiAssistantEnabled(this)
        val showAsset = AssetDisplayHelper.getShowAsset(this)
        val menu = bottomNavigationView.menu
        val aiItem = menu.findItem(R.id.nav_agent)
        val assetItem = menu.findItem(R.id.nav_asset)
        val separateAi = showAiAssistant && BottomNavigationPreferences.getAiEntryStyle(this) == AiEntryStyle.SEPARATE_CIRCLE
        aiItem.isVisible = showAiAssistant && !separateAi
        aiNavigationButton.visibility = if (separateAi) View.VISIBLE else View.GONE
        assetItem.isVisible = showAsset
    }

    private fun applyBottomNavigationOrder() {
        val menu = bottomNavigationView.menu
        val order = BottomNavigationPreferences.getMenuOrder(this)
        if ((0 until menu.size()).map { menu.getItem(it).itemId } == order.map { it.menuId }) return
        val selected = (0 until menu.size()).map { menu.getItem(it) }
            .firstOrNull { it.isChecked }?.itemId ?: R.id.nav_ledger
        // Check items directly instead of invoking selectedItemId: sorting must never navigate.
        menu.clear()
        order.forEachIndexed { index, destination ->
            menu.add(0, destination.menuId, index, destination.titleRes).apply {
                setIcon(destination.iconRes)
                isCheckable = true
            }
        }
        menu.findItem(selected)?.isChecked = true
    }

    private fun syncNavigationSelection() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val destination = when (fragment) {
            is LedgerFragment -> R.id.nav_ledger
            is StatisticsFragment -> R.id.nav_statistics
            is AssetFragment -> R.id.nav_asset
            is AgentFragment -> R.id.nav_agent
            is SettingsFragment -> R.id.nav_settings
            else -> null
        }
        // Keep the hidden native AI item checked: visible menu destinations must not look selected.
        destination?.let { bottomNavigationView.menu.findItem(it).isChecked = true }
        val externalSelected = aiNavigationButton.visibility == View.VISIBLE && fragment is AgentFragment
        aiNavigationButton.isSelected = externalSelected
        (bottomNavigationView as? com.example.cardtally.view.ElasticBottomNavigationView)
            ?.setExternalDestinationSelected(externalSelected)
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
        updateBottomNavigationVisibility()
        syncNavigationSelection()
        val docked = BottomNavigationPreferences.isDocked(this)
        val side = if (docked) 0 else (21 * resources.displayMetrics.density + 0.5f).toInt()
        val top = if (docked) 0 else (8 * resources.displayMetrics.density + 0.5f).toInt()
        val bottom = if (docked) 0 else resources.getDimensionPixelSize(R.dimen.floating_nav_shell_bottom_padding)
        navShell.setPaddingRelative(side, top, side, bottom)
        val separateAi = aiNavigationButton.visibility == View.VISIBLE
        val aiOnLeft = BottomNavigationPreferences.getAiEntryPosition(this) == AiEntryPosition.LEFT
        val shell = navShell as android.widget.LinearLayout
        val aiIndex = if (aiOnLeft) 0 else shell.childCount - 1
        if (shell.indexOfChild(aiNavigationButton) != aiIndex) {
            // Move the existing destination view, keeping its selection and click/accessibility behavior.
            shell.removeView(aiNavigationButton)
            shell.addView(aiNavigationButton, aiIndex)
        }
        val aiParams = aiNavigationButton.layoutParams as android.widget.LinearLayout.LayoutParams
        val edge = if (docked) (8 * resources.displayMetrics.density + .5f).toInt() else 0
        val gap = (12 * resources.displayMetrics.density + .5f).toInt()
        aiParams.marginStart = if (aiOnLeft) edge else gap
        aiParams.marginEnd = if (aiOnLeft) gap else edge
        aiParams.topMargin = if (docked) (4 * resources.displayMetrics.density + .5f).toInt() else 0
        aiParams.bottomMargin = aiParams.topMargin
        aiNavigationButton.layoutParams = aiParams
        // Docked mode still paints one full-width bottom card, including the space around AI.
        navShell.background = if (docked && separateAi) android.graphics.drawable.GradientDrawable().apply {
            setColor(com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(this@MainActivity) or 0xFF000000.toInt())
            val radius = 12 * resources.displayMetrics.density
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        } else null
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
