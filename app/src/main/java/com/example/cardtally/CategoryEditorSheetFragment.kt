package com.example.cardtally

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.example.cardtally.adapter.IconPickerDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategorySheetUi as Ui
import com.example.cardtally.util.TablerIconCatalog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryEditorSheetFragment : BottomSheetDialogFragment() {
    private var original: Category? = null
    private var icon: String? = null
    private var color: String? = null
    private var input: EditText? = null
    private val children = mutableSetOf<Dialog>()
    private val categoryId get() = requireArguments().getLong("id")
    private fun dirty() = input != null && (input!!.text.toString() != original?.name.orEmpty() ||
        icon != (original?.icon ?: if (categoryId == 0L) "tabler_category" else null) || color != initialColor())
    private fun initialColor() = original?.color ?: if (categoryId == 0L) {
        if (requireArguments().getInt("type") == 0) "#E8F5E9" else "#FCE4EC"
    } else null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = object : BottomSheetDialog(requireContext(), theme) {
        override fun cancel() { requestClose() }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        original = DatabaseHelper(requireContext()).use { it.getCategoryById(categoryId) }
        if (categoryId > 0L && original == null) { dismiss(); return View(requireContext()) }
        icon = if (savedInstanceState != null) savedInstanceState.getString("icon")
            else if (categoryId > 0) original?.icon else "tabler_category"
        color = if (savedInstanceState != null) savedInstanceState.getString("color") else initialColor()
        val content = Ui.content(requireContext(), getString(if (categoryId > 0) R.string.category_edit_title else R.string.category_add_title), ::requestClose)
        val path = DatabaseHelper(requireContext()).use { db ->
            if (categoryId > 0) db.buildCategoryPathLabel(categoryId) else db.buildCategoryPathLabel(requireArguments().getLong("parent"))
        }.orEmpty()
        val typeLabel = getString(if ((original?.type ?: requireArguments().getInt("type")) == 0) R.string.record_type_expense else R.string.record_type_income)
        content.body.addView(Ui.text(requireContext(), if (path.isBlank()) typeLabel else "$typeLabel / $path", 14f))
        val row = LinearLayout(requireContext()).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val image = androidx.appcompat.widget.AppCompatImageButton(requireContext()).apply {
            contentDescription = getString(R.string.category_dialog_icon_hint)
            val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = attrs.getDrawable(0); attrs.recycle()
            setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12))
        }
        fun render() {
            val base = Ui.color(requireContext(), com.google.android.material.R.attr.colorSurface)
            val background = color?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: base
            val opaque = ColorUtils.compositeColors(background, base)
            val light = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            val dark = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
            image.background = GradientDrawable().apply { setColor(background); cornerRadius = Ui.dp(requireContext(), 12).toFloat() }
            val resource = icon?.let { TablerIconCatalog.resourceId(requireContext(), it) } ?: 0
            image.setImageResource(resource.takeIf { it != 0 } ?: R.drawable.tabler_category)
            image.imageTintList = ColorStateList.valueOf(if (ColorUtils.calculateContrast(light, opaque) > ColorUtils.calculateContrast(dark, opaque)) light else dark)
        }
        image.setOnClickListener {
            val picker = IconPickerDialog.show(requireContext(), icon) { selected ->
                if (view != null && selected != null) { icon = selected; render() }
            }
            children += picker
            picker.setOnDismissListener { children -= picker }
        }
        content.body.addView(Ui.text(requireContext(), getString(R.string.category_name_label), 14f))
        row.addView(image, LinearLayout.LayoutParams(Ui.dp(requireContext(), 52), Ui.dp(requireContext(), 52)))
        input = EditText(requireContext()).apply {
            id = R.id.edit_category_name; isSaveEnabled = false
            setText(savedInstanceState?.getString("name") ?: original?.name.orEmpty())
            hint = getString(R.string.category_dialog_name_hint); setSingleLine(true); textSize = 16f
            minHeight = Ui.dp(context, 52)
            setTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            setBackgroundResource(R.drawable.bg_input_surface)
            backgroundTintList = null
            setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12))
        }
        row.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Ui.dp(requireContext(), 12) })
        content.body.addView(row)
        content.body.addView(Ui.text(requireContext(), getString(R.string.category_add_color_title), 14f))
        val swatches = LinearLayout(requireContext())
        fun renderColors() {
            swatches.removeAllViews()
            for (value in listOf("#FCE4EC", "#E3F2FD", "#E8F5E9", "#FFF3E0", "#F3E5F5", "#F5F5F5")) {
                swatches.addView(androidx.appcompat.widget.AppCompatTextView(requireContext()).apply {
                    text = if (color == value) "✓" else ""; textSize = 18f; gravity = android.view.Gravity.CENTER
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.black)); contentDescription = getString(R.string.category_color_choice, value)
                    isSelected = color == value; isClickable = true; isFocusable = true
                    background = GradientDrawable().apply { setColor(Color.parseColor(value)); cornerRadius = Ui.dp(context, 12).toFloat() }
                    setOnClickListener { color = value; render(); renderColors() }
                }, LinearLayout.LayoutParams(Ui.dp(requireContext(), 48), Ui.dp(requireContext(), 48)).apply { marginEnd = Ui.dp(requireContext(), 4) })
            }
        }
        content.body.addView(android.widget.HorizontalScrollView(requireContext()).apply { addView(swatches) })
        if (original != null) {
            val actions = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(androidx.core.content.ContextCompat.getColor(context, R.color.surface_container_low))
                    cornerRadius = Ui.dp(context, 12).toFloat()
                }
            }
            actions.addView(Ui.button(requireContext(), getString(R.string.category_merge_action), navigation = true) { requestAction("merge") })
            actions.addView(Ui.button(requireContext(), getString(R.string.category_delete_title), danger = true, navigation = true) { requestAction("delete") })
            content.body.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(requireContext(), 16)
            })
        }
        content.footer.addView(Ui.button(requireContext(), getString(R.string.category_save), primary = true) {
            val name = input?.text.toString().trim()
            if (name.isBlank()) { input?.error = getString(R.string.category_error_empty_name); return@button }
            try {
                DatabaseHelper(requireContext()).use { db ->
                    val item = original?.copy() ?: Category(type = requireArguments().getInt("type"), parentId = requireArguments().getLong("parent").takeIf { it > 0 })
                    item.name = name; item.icon = icon; item.color = color
                    check(if (original == null) db.addCategory(item) > 0 else db.updateCategory(item) > 0)
                }
                parentFragmentManager.setFragmentResult(CategoryManageFragment.CATEGORY_SAVED_RESULT, Bundle())
                dismiss()
            } catch (e: DatabaseHelper.CategoryOperationException) {
                Toast.makeText(requireContext(), getString(R.string.category_editor_save_failed), Toast.LENGTH_LONG).show()
            } catch (_: android.database.sqlite.SQLiteException) {
                Toast.makeText(requireContext(), R.string.category_editor_save_failed, Toast.LENGTH_LONG).show()
            } catch (_: IllegalStateException) {
                Toast.makeText(requireContext(), R.string.category_editor_save_failed, Toast.LENGTH_LONG).show()
            }
        })
        render(); renderColors()
        return content.root
    }
    private fun ask(message: Int, action: () -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val content = Ui.content(requireContext(), getString(R.string.category_unsaved_title)) { dialog.dismiss() }
        content.body.addView(Ui.text(requireContext(), getString(message)))
        content.footer.addView(Ui.button(requireContext(), getString(R.string.category_continue), primary = true) { dialog.dismiss(); action() })
        content.footer.addView(Ui.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() })
        dialog.setContentView(content.root); children += dialog
        dialog.setOnDismissListener { children -= dialog }
        dialog.show(); Ui.configure(dialog, content.root)
    }
    private fun requestClose() { if (dirty()) ask(R.string.category_discard_message) { dismiss() } else dismiss() }
    private fun requestAction(action: String) {
        val send = { parentFragmentManager.setFragmentResult(ACTION, Bundle().apply { putLong("id", categoryId); putString("action", action) }) }
        if (dirty()) ask(R.string.category_action_unsaved_message, send) else send()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", input?.text?.toString()); outState.putString("icon", icon); outState.putString("color", color)
    }
    override fun onStart() {
        super.onStart()
        view?.let { Ui.configure(dialog as BottomSheetDialog, it) }
        (dialog as? BottomSheetDialog)?.behavior?.isDraggable = false
    }
    override fun onDestroyView() { children.toList().forEach { it.dismiss() }; children.clear(); input = null; super.onDestroyView() }
    companion object {
        const val TAG = "category_editor_sheet"
        const val ACTION = "category_editor_action"
        fun newInstance(type: Int, parentId: Long?, id: Long = 0L) = CategoryEditorSheetFragment().apply {
            arguments = Bundle().apply { putInt("type", type); putLong("parent", parentId ?: 0L); putLong("id", id) }
        }
    }
}
