package com.example.cardtally

import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.WallpaperGalleryAdapter
import com.example.cardtally.util.ThemeHelper
import com.example.cardtally.util.WallpaperHelper
import com.google.android.material.slider.Slider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** A secondary destination; theme preferences and application remain owned by ThemeHelper. */
class AppearanceSettingsFragment : Fragment() {
    private var opacityApplyTask: Runnable? = null
    private var wallpaperExecutor: ExecutorService? = null
    private var wallpaperImport: Future<*>? = null
    private var wallpaperLibraryLoad: Future<*>? = null
    private var wallpaperGallery: WallpaperGalleryAdapter? = null
    private var importingWallpaper = false
    private var syncingSelection = false
    private var appearanceReady = false
    private val chooseWallpaper = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importWallpaper(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_appearance_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.text_title).apply {
            setText(R.string.settings_theme)
            ViewCompat.setAccessibilityHeading(this, true)
        }
        view.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        WallpaperHelper.bindImage(view.findViewById<ImageView>(R.id.image_wallpaper_preview))
        view.findViewById<View>(R.id.button_choose_wallpaper).setOnClickListener {
            chooseWallpaper.launch(arrayOf("image/*"))
        }
        view.findViewById<View>(R.id.button_reset_wallpaper).apply {
            isEnabled = WallpaperHelper.hasCustomImage(requireContext())
            setOnClickListener {
                runWallpaperUpdate(view, R.string.theme_wallpaper_reset_failed, R.string.theme_wallpaper_applying) {
                    WallpaperHelper.restoreDefault(it)
                }
            }
        }
        listOf(R.id.text_solid_background, R.id.text_picture_background).forEach { id ->
            ViewCompat.setAccessibilityHeading(view.findViewById(id), true)
        }
        configureCardOpacity(view)
        configureAppearanceChoices(view)
        configureWallpaperGallery(view, restoringState = savedInstanceState != null)
    }

    private fun configureAppearanceChoices(root: View) {
        syncAppearanceChoices(root)
        root.findViewById<RadioGroup>(R.id.group_theme).setOnCheckedChangeListener { _, checkedId ->
            if (!appearanceReady || syncingSelection || importingWallpaper) return@setOnCheckedChangeListener
            val mode = when (checkedId) {
                R.id.radio_theme_light -> ThemeHelper.THEME_LIGHT
                R.id.radio_theme_dark -> ThemeHelper.THEME_DARK
                R.id.radio_theme_system -> ThemeHelper.THEME_SYSTEM
                else -> return@setOnCheckedChangeListener
            }
            applyAppearance(root, mode)
        }
        root.findViewById<RadioGroup>(R.id.group_wallpaper_palette).setOnCheckedChangeListener { _, checkedId ->
            if (!appearanceReady || syncingSelection || importingWallpaper) return@setOnCheckedChangeListener
            val palette = when (checkedId) {
                R.id.radio_wallpaper_light -> ThemeHelper.THEME_LIGHT
                R.id.radio_wallpaper_dark -> ThemeHelper.THEME_DARK
                R.id.radio_wallpaper_system -> ThemeHelper.THEME_SYSTEM
                else -> return@setOnCheckedChangeListener
            }
            applyAppearance(root, ThemeHelper.THEME_WALLPAPER, palette)
        }
    }

    private fun syncAppearanceChoices(root: View) {
        syncingSelection = true
        try {
            val solid = root.findViewById<RadioGroup>(R.id.group_theme)
            val picture = root.findViewById<RadioGroup>(R.id.group_wallpaper_palette)
            when (ThemeHelper.getTheme(requireContext())) {
                ThemeHelper.THEME_WALLPAPER -> {
                    solid.clearCheck()
                    picture.check(when (ThemeHelper.getWallpaperPalette(requireContext())) {
                        ThemeHelper.THEME_LIGHT -> R.id.radio_wallpaper_light
                        ThemeHelper.THEME_SYSTEM -> R.id.radio_wallpaper_system
                        else -> R.id.radio_wallpaper_dark
                    })
                }
                else -> {
                    picture.clearCheck()
                    solid.check(when (ThemeHelper.getTheme(requireContext())) {
                        ThemeHelper.THEME_DARK -> R.id.radio_theme_dark
                        ThemeHelper.THEME_SYSTEM -> R.id.radio_theme_system
                        else -> R.id.radio_theme_light
                    })
                }
            }
        } finally {
            syncingSelection = false
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        // Ignore checked-change callbacks until framework hierarchy restoration has finished.
        super.onViewStateRestored(savedInstanceState)
        view?.let { root ->
            syncAppearanceChoices(root)
            root.findViewById<Slider>(R.id.slider_card_opacity).value = ThemeHelper.getCardOpacity(requireContext()).toFloat()
        }
        appearanceReady = true
    }

    private fun applyAppearance(root: View, mode: Int, palette: Int = ThemeHelper.getWallpaperPalette(requireContext())) {
        opacityApplyTask?.let(root::removeCallbacks)
        // One shared opacity: always carry the currently displayed slider value into the next palette.
        ThemeHelper.saveCardOpacity(requireContext(), root.findViewById<Slider>(R.id.slider_card_opacity).value.toInt())
        if (mode == ThemeHelper.THEME_WALLPAPER) ThemeHelper.saveWallpaperPalette(requireContext(), palette)
        ThemeHelper.saveTheme(requireContext(), mode)
        syncAppearanceChoices(root)
        requireActivity().recreate()
    }

    private fun ensureWallpaperExecutor(): ExecutorService = wallpaperExecutor
        ?: Executors.newSingleThreadExecutor().also { wallpaperExecutor = it }

    private fun configureWallpaperGallery(root: View, restoringState: Boolean) {
        val list = root.findViewById<RecyclerView>(R.id.recycler_wallpaper_images)
        val adapter = WallpaperGalleryAdapter(onSelect = { id ->
            if (!importingWallpaper) {
                runWallpaperUpdate(root, R.string.theme_wallpaper_select_failed, R.string.theme_wallpaper_applying) {
                    WallpaperHelper.selectImage(it, id)
                }
            }
        }, onDelete = { id ->
            runWallpaperUpdate(root, R.string.theme_wallpaper_delete_failed, R.string.theme_wallpaper_deleting,
                enableWallpaper = false) {
                WallpaperHelper.deleteImage(it, id)
            }
        })
        wallpaperGallery = adapter
        list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        list.adapter = adapter
        list.itemAnimator = null
        val appContext = requireContext().applicationContext
        wallpaperLibraryLoad = ensureWallpaperExecutor().submit {
            val images = WallpaperHelper.listImages(appContext)
            val selected = WallpaperHelper.currentImageId(appContext)
            if (Thread.currentThread().isInterrupted) return@submit
            root.post {
                if (view !== root || !isAdded || wallpaperGallery !== adapter) return@post
                adapter.submitImages(images, selected)
                // Recreated views restore their existing gallery offset after async data arrives.
                if (!restoringState) {
                    list.scrollToPosition(images.indexOfFirst { it.id == selected }.coerceAtLeast(0))
                }
            }
        }
    }

    private fun importWallpaper(uri: Uri) {
        val root = view ?: return
        runWallpaperUpdate(root, R.string.theme_wallpaper_import_failed, R.string.theme_wallpaper_importing) {
            WallpaperHelper.importImage(it, uri)
        }
    }

    private fun runWallpaperUpdate(
        root: View, errorRes: Int, busyRes: Int,
        enableWallpaper: Boolean = true,
        action: (android.content.Context) -> Unit
    ) {
        if (importingWallpaper || view !== root || !isAdded) return
        val appContext = requireContext().applicationContext
        opacityApplyTask?.let(root::removeCallbacks)
        setWallpaperImporting(root, true, busyRes)
        wallpaperImport = ensureWallpaperExecutor().submit {
            val success = try {
                action(appContext)
                true
            } catch (_: Exception) {
                false
            } catch (_: OutOfMemoryError) {
                false
            }
            if (Thread.currentThread().isInterrupted) return@submit
            root.post {
                if (view !== root || !isAdded) return@post
                setWallpaperImporting(root, false)
                if (success) {
                    if (enableWallpaper) applyWallpaper(root) else requireActivity().recreate()
                } else Toast.makeText(requireContext(), errorRes, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setWallpaperImporting(root: View, importing: Boolean, busyRes: Int = R.string.theme_wallpaper_importing) {
        importingWallpaper = importing
        wallpaperGallery?.setSelectionEnabled(!importing)
        root.findViewById<TextView>(R.id.button_choose_wallpaper).apply {
            isEnabled = !importing
            setText(if (importing) busyRes else R.string.theme_wallpaper_choose)
        }
        root.findViewById<View>(R.id.button_reset_wallpaper).isEnabled =
            !importing && WallpaperHelper.hasCustomImage(requireContext())
        listOf(R.id.group_theme, R.id.group_wallpaper_palette).forEach { id ->
            val group = root.findViewById<RadioGroup>(id)
            for (index in 0 until group.childCount) {
                (group.getChildAt(index) as? RadioButton)?.isEnabled = !importing
            }
        }
        root.findViewById<Slider>(R.id.slider_card_opacity).isEnabled =
            !importing && ThemeHelper.getTheme(requireContext()) == ThemeHelper.THEME_WALLPAPER
    }

    private fun applyWallpaper(root: View) = applyAppearance(root, ThemeHelper.THEME_WALLPAPER)

    private fun configureCardOpacity(view: View) {
        val slider = view.findViewById<Slider>(R.id.slider_card_opacity)
        val label = view.findViewById<TextView>(R.id.text_card_opacity)
        val appliedOpacity = ThemeHelper.getCardOpacity(requireContext())
        var trackingTouch = false
        slider.value = appliedOpacity.toFloat()
        slider.isEnabled = ThemeHelper.getTheme(requireContext()) == ThemeHelper.THEME_WALLPAPER
        slider.setLabelFormatter { "${it.toInt()}%" }
        fun updateLabel(opacity: Int) {
            label.text = getString(R.string.theme_card_opacity_value, opacity)
        }
        updateLabel(appliedOpacity)
        val applyTask = Runnable {
            if (isAdded && ThemeHelper.getTheme(requireContext()) == ThemeHelper.THEME_WALLPAPER &&
                ThemeHelper.getCardOpacity(requireContext()) != appliedOpacity) {
                requireActivity().recreate()
            }
        }
        opacityApplyTask = applyTask
        slider.addOnChangeListener { _, value, fromUser ->
            updateLabel(value.toInt())
            if (fromUser) {
                ThemeHelper.saveCardOpacity(requireContext(), value.toInt())
                view.removeCallbacks(applyTask)
                // Touch applies on release; keyboard/TalkBack changes apply after a short pause.
                if (!trackingTouch) view.postDelayed(applyTask, 400L)
            }
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                trackingTouch = true
                view.removeCallbacks(applyTask)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                trackingTouch = false
                view.removeCallbacks(applyTask)
                applyTask.run()
            }
        })
    }

    override fun onDestroyView() {
        appearanceReady = false
        opacityApplyTask?.let { view?.removeCallbacks(it) }
        opacityApplyTask = null
        wallpaperImport?.cancel(true)
        wallpaperImport = null
        wallpaperLibraryLoad?.cancel(true)
        wallpaperLibraryLoad = null
        view?.findViewById<RecyclerView>(R.id.recycler_wallpaper_images)?.adapter = null
        wallpaperGallery?.close()
        wallpaperGallery = null
        wallpaperExecutor?.shutdownNow()
        wallpaperExecutor = null
        importingWallpaper = false
        super.onDestroyView()
    }
}
