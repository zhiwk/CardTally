package com.example.cardtally

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.adapter.FeaturedIconLabels
import com.example.cardtally.adapter.IconBrowserBinder
import com.example.cardtally.adapter.IconPickerDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.TablerIconCatalog

class AddCategoryFragment : Fragment() {
    companion object {
        private const val ARG_TYPE = "category_type"
        private const val ARG_PARENT_ID = "category_parent_id"
        private val COLORS = listOf(
            "#FCE4EC", "#E3F2FD", "#E8F5E9",
            "#FFF3E0", "#F3E5F5", "#F5F5F5"
        )

        fun newInstance(type: Int, parentId: Long?) = AddCategoryFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, type)
                parentId?.let { putLong(ARG_PARENT_ID, it) }
            }
        }
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var nameInput: EditText
    private lateinit var iconView: ImageView
    private lateinit var preview: FrameLayout
    private lateinit var iconLibraryPane: View
    private var iconBrowser: IconBrowserBinder? = null
    private var selectedIcon = "tabler_category"
    private var selectedColor = COLORS.first()
    private val type get() = arguments?.getInt(ARG_TYPE, 0) ?: 0
    private val parentId get() = arguments?.getLong(ARG_PARENT_ID, -1L)?.takeIf { it > 0L }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_category, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        selectedColor = if (type == 0) "#E8F5E9" else "#FCE4EC"
        nameInput = view.findViewById(R.id.add_category_name)
        iconView = view.findViewById(R.id.add_category_icon)
        preview = view.findViewById(R.id.add_category_preview)
        iconLibraryPane = view.findViewById(R.id.icon_library_pane)
        view.findViewById<TextView>(R.id.add_category_title).text = getString(
            if (type == 0) R.string.category_add_expense_title else R.string.category_add_income_title
        )
        view.findViewById<View>(R.id.add_category_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.add_category_confirm_top).setOnClickListener { saveCategory() }
        view.findViewById<View>(R.id.add_category_icon_picker).setOnClickListener { showIconPicker() }

        val labels = FeaturedIconLabels.load(requireContext())
        iconBrowser = IconBrowserBinder(requireContext(), view, labels) { icon ->
            selectedIcon = icon
            renderIcon()
        }.also { it.bind(selectedIcon) }

        renderIcon()
        renderColors(view.findViewById(R.id.add_category_colors))
        clampBrowseAreaToViewport()
        return view
    }

    /**
     * Caps the icon browse area to the space that is actually reachable on
     * screen instead of an estimated offset. The pane lives inside a scrolling
     * page, so its real window position is used: everything between the pane top
     * and the bottom of the visible content area (excluding system insets and
     * the page's own bottom margin) becomes the pane height, clamped to the
     * resource maximum. Re-runs on layout/font/orientation changes and detaches
     * with the view, and never grows the pane beyond the declared maximum.
     */
    private fun clampBrowseAreaToViewport() {
        val density = resources.displayMetrics.density
        val maxHeightPx = resources.getDimensionPixelSize(R.dimen.category_icon_browse_height)
        val bottomMarginPx = (16 * density).toInt()

        fun updatePaneHeight() {
            val pane = view?.findViewById<View>(R.id.icon_library_pane) ?: return
            val location = IntArray(2)
            pane.getLocationInWindow(location)
            val bounds = android.graphics.Rect()
            pane.getWindowVisibleDisplayFrame(bounds)
            val availablePx = (bounds.bottom - bottomMarginPx - location[1]).coerceAtMost(maxHeightPx)
            if (availablePx <= 0) return

            val params = pane.layoutParams ?: return
            if (params.height != availablePx) {
                params.height = availablePx
                pane.layoutParams = params
            }
        }

        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener { updatePaneHeight() }
        iconLibraryPane.viewTreeObserver.addOnGlobalLayoutListener(listener)
        iconLibraryPane.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) {
                    v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        })
        iconLibraryPane.post { updatePaneHeight() }
    }

    private fun renderIcon() {
        val resourceId = TablerIconCatalog.resourceId(requireContext(), selectedIcon)
        iconView.setImageResource(resourceId.takeIf { it != 0 } ?: R.drawable.tabler_category)
        preview.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(selectedColor))
        }
    }

    private fun renderColors(container: LinearLayout) {
        container.removeAllViews()
        COLORS.forEach { color ->
            val swatch = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp).apply { marginEnd = 12.dp }
                tag = color
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(color)) }
                contentDescription = color
                setOnClickListener {
                    selectedColor = color
                    renderIcon()
                    renderColors(container)
                }
            }
            if (color == selectedColor) {
                swatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                    setStroke(2.dp, requireContext().getColor(com.example.cardtally.R.color.onSurface_light))
                }
            }
            container.addView(swatch)
        }
    }

    private fun showIconPicker() {
        IconPickerDialog.show(requireContext(), initialSelectedIcon = selectedIcon) { icon ->
            if (icon != null) {
                selectedIcon = icon
                renderIcon()
                iconBrowser?.updateSelection(icon)
            }
        }
    }

    private fun saveCategory() {
        val name = nameInput.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            nameInput.error = getString(R.string.category_error_empty_name)
            return
        }
        try {
            val id = databaseHelper.addCategory(Category(name = name, type = type, icon = selectedIcon, color = selectedColor, parentId = parentId))
            if (id == -1L) {
                Toast.makeText(requireContext(), R.string.category_add_failed, Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(requireContext(), R.string.category_add_success, Toast.LENGTH_SHORT).show()
            parentFragmentManager.setFragmentResult(
                CategoryManageFragment.CATEGORY_SAVED_RESULT,
                Bundle()
            )
            parentFragmentManager.popBackStack()
        } catch (exception: DatabaseHelper.CategoryOperationException) {
            Toast.makeText(requireContext(), exception.message ?: getString(R.string.category_add_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

}
