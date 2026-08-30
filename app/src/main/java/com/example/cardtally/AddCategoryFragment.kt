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
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.IconPickerAdapter
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
    private lateinit var iconLibrary: LinearLayout
    private var selectedIcon = "tabler_category"
    // Expense categories default to the app's success green; income categories
    // use the income red. Users can still choose another swatch before saving.
    private var selectedColor = COLORS.first()
    private var selectedGroupIndex = 0
    private val type get() = arguments?.getInt(ARG_TYPE, 0) ?: 0
    private val parentId get() = arguments?.getLong(ARG_PARENT_ID, -1L)?.takeIf { it > 0L }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_category, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        selectedColor = if (type == 0) "#E8F5E9" else "#FCE4EC"
        nameInput = view.findViewById(R.id.add_category_name)
        iconView = view.findViewById(R.id.add_category_icon)
        preview = view.findViewById(R.id.add_category_preview)
        iconLibrary = view.findViewById(R.id.add_category_icon_library)
        view.findViewById<TextView>(R.id.add_category_title).text = getString(
            if (type == 0) R.string.category_add_expense_title else R.string.category_add_income_title
        )
        view.findViewById<View>(R.id.add_category_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.add_category_confirm_top).setOnClickListener { saveCategory() }
        view.findViewById<View>(R.id.add_category_icon_picker).setOnClickListener { showIconPicker() }
        renderIcon()
        renderColors(view.findViewById(R.id.add_category_colors))
        renderIconLibrary()
        return view
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_icon_picker, null)
        val dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.category_icon_picker_title)
            .setView(dialogView).setNegativeButton(R.string.dialog_cancel, null).create()
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recycler_icons)
        val adapter = IconPickerAdapter(TablerIconCatalog.icons, selectedIcon) { icon ->
            selectedIcon = icon ?: selectedIcon
            renderIcon()
            dialog.dismiss()
        }
        recycler.adapter = adapter
        dialog.show()
    }

    private fun renderIconLibrary() {
        iconLibrary.removeAllViews()
        val context = requireContext()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val navigation = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(64.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        TablerIconCatalog.groups.forEachIndexed { index, group ->
            navigation.addView(TextView(context).apply {
                text = group.title
                textSize = 14f
                gravity = android.view.Gravity.CENTER_VERTICAL
                setTextColor(context.getColor(if (index == selectedGroupIndex) R.color.onSurface_light else R.color.onSurfaceVariant_light))
                setTypeface(typeface, if (index == selectedGroupIndex) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setPadding(4.dp, 0, 4.dp, 0)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 52.dp)
                isClickable = true
                setOnClickListener {
                    selectedGroupIndex = index
                    renderIconLibrary()
                }
            })
        }
        content.addView(navigation)
        val grid = GridLayout(context).apply {
            columnCount = 4
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        TablerIconCatalog.groups.getOrNull(selectedGroupIndex)?.icons?.distinct()?.take(16)?.forEach { icon ->
            grid.addView(createIconTile(icon))
        }
        content.addView(grid)
        iconLibrary.addView(content)
        iconLibrary.addView(TextView(context).apply {
            text = getString(R.string.category_icon_more)
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(context.getColor(R.color.primary_light))
            setPadding(0, 8.dp, 0, 8.dp)
            setOnClickListener { showIconPicker() }
        })
    }

    private fun createIconTile(icon: String): View {
        val context = requireContext()
        val tile = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            minimumHeight = 64.dp
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(2.dp, 2.dp, 2.dp, 2.dp)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedIcon = icon
                renderIcon()
                renderIconLibrary()
            }
        }
        tile.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(if (icon == selectedIcon) "#DDF3F4" else "#F2F3F4"))
            }
            setPadding(9.dp, 9.dp, 9.dp, 9.dp)
            setImageResource(TablerIconCatalog.resourceId(context, icon).takeIf { it != 0 } ?: R.drawable.tabler_category)
            imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.onSurface_light))
        })
        tile.addView(TextView(context).apply {
            text = icon.removePrefix("tabler_").replace('_', ' ')
            textSize = 10f
            setTextColor(context.getColor(R.color.onSurfaceVariant_light))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        return tile
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
