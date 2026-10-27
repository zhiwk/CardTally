package com.example.cardtally

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsAnimationCompat
import com.example.cardtally.adapter.IconPickerDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategorySheetUi as Ui
import com.example.cardtally.util.TablerIconCatalog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** A single modal owns the leaf draft, parent selection and optional unsaved parent. */
class RecordCategoryAddSheetFragment : BottomSheetDialogFragment() {
    private val type get() = requireArguments().getInt("type")
    private var name = ""
    private var icon = "tabler_category"
    private var parentId: Long? = null
    private var parentName = ""
    private var parentIcon = "tabler_category"
    private var choosing = false
    private var pendingId: Long? = null
    private var pendingName = ""
    private var pendingIcon = "tabler_category"
    private var newParent = false
    private var saving = false
    private var savedId = 0L
    private var picker: Dialog? = null
    private var content: Ui.Content? = null
    private var pendingIconListener: ViewTreeObserver.OnPreDrawListener? = null
    private val imeAnimations = mutableSetOf<WindowInsetsAnimationCompat>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val state = savedInstanceState
        name = state?.getString("name").orEmpty()
        icon = state?.getString("icon") ?: "tabler_category"
        parentId = (state?.getLong("parent") ?: requireArguments().getLong("parent")).takeIf { it > 0 }
        parentName = state?.getString("parent_name").orEmpty()
        parentIcon = state?.getString("parent_icon") ?: "tabler_category"
        choosing = state?.getBoolean("choosing") ?: false
        pendingId = state?.getLong("pending")?.takeIf { it > 0 }
        pendingName = state?.getString("pending_name").orEmpty()
        pendingIcon = state?.getString("pending_icon") ?: "tabler_category"
        newParent = state?.getBoolean("new_parent") ?: false
        savedId = state?.getLong("saved_id") ?: 0L
        val title = getString(if (type == 0) R.string.record_category_add_expense else R.string.record_category_add_income)
        val ui = Ui.content(requireContext(), title) { dismiss() }
        content = ui
        render()
        return ui.root
    }

    private fun parents(): List<Category> = DatabaseHelper(requireContext()).use { it.getRecordCategoryParents(type) }

    private fun input(value: String, hintText: String, changed: (String) -> Unit) = EditText(requireContext()).apply {
        isSaveEnabled = false; setSingleLine(true); textSize = 16f
        minHeight = Ui.dp(context, 48)
        setText(value); hint = hintText
        setTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurface))
        setHintTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
        doAfterTextChanged { changed(it.toString()) }
    }

    private fun render() {
        val ui = content ?: return
        cancelPendingIcon()
        ui.root.findFocus()?.let {
            androidx.core.view.ViewCompat.getWindowInsetsController(it)?.hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        ui.root.isFocusableInTouchMode = true
        ui.root.requestFocus()
        ui.body.removeAllViews(); ui.footer.removeAllViews()
        if (choosing) { renderParents(ui); return }
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_add_hint), 14f))
        ui.body.addView(Ui.text(requireContext(), getString(R.string.category_name_label), 14f))
        val saveButton = Ui.button(requireContext(), getString(R.string.record_category_add_use), primary = true) { save() }
        val row = LinearLayout(requireContext()).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val image = iconButton(icon).apply {
            id = R.id.button_record_category_leaf_icon
            setOnClickListener {
                openIconPicker(icon) { selected -> icon = selected; setImageResource(iconResource(selected)) }
            }
        }
        row.addView(image, LinearLayout.LayoutParams(Ui.dp(requireContext(), 48), Ui.dp(requireContext(), 48)))
        row.addView(input(name, getString(R.string.category_dialog_name_hint)) {
            name = it; saveButton.isEnabled = validDraft()
        }.apply { id = R.id.edit_category_name }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Ui.dp(requireContext(), 12)
        })
        ui.body.addView(row)
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_parent_label), 14f))
        val selected = parents().firstOrNull { it.id == parentId }
        if (selected == null && parentId != null) parentId = null
        val label = selected?.name ?: parentName.takeIf { parentId == null && it.isNotBlank() }
        ui.body.addView(LinearLayout(requireContext()).apply {
            id = R.id.button_record_category_choose_parent
            gravity = android.view.Gravity.CENTER_VERTICAL
            minimumHeight = Ui.dp(context, 56)
            setPadding(Ui.dp(context, 12), Ui.dp(context, 8), Ui.dp(context, 12), Ui.dp(context, 8))
            isClickable = true; isFocusable = true
            val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            background = attrs.getDrawable(0); attrs.recycle()
            if (label != null) addView(ImageView(context).apply {
                setImageResource(iconResource(if (selected != null) selected.icon else parentIcon))
                imageTintList = ColorStateList.valueOf(Ui.color(context, com.google.android.material.R.attr.colorOnSurface))
            }, LinearLayout.LayoutParams(Ui.dp(context, 24), Ui.dp(context, 24)).apply { marginEnd = Ui.dp(context, 12) })
            addView(Ui.text(context, label ?: getString(R.string.record_category_choose_parent)), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_arrow_right)
                imageTintList = ColorStateList.valueOf(Ui.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            }, LinearLayout.LayoutParams(Ui.dp(context, 20), Ui.dp(context, 20)))
            setOnClickListener {
                pendingId = parentId; pendingName = parentName; pendingIcon = parentIcon
                newParent = parentId == null && parentName.isNotBlank()
                choosing = true; render()
            }
        })
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_parent_hint), 14f))
        saveButton.isEnabled = validDraft()
        ui.footer.addView(saveButton)
    }

    private fun validDraft() = !saving && savedId == 0L && name.isNotBlank() &&
        (parentId != null || parentName.isNotBlank())

    private fun renderParents(ui: Ui.Content) {
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_choose_parent), 16f))
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_parent_picker_hint), 14f))
        val options = parents()
        val use = Ui.button(requireContext(), getString(R.string.record_category_use_parent), primary = true) {
            parentId = if (newParent) null else pendingId
            parentName = if (newParent) pendingName.trim() else ""
            if (newParent) parentIcon = pendingIcon
            choosing = false; render()
        }
        val cells = mutableListOf<ParentCell>()
        lateinit var newNameInput: EditText
        fun refresh() {
            use.isEnabled = if (newParent) pendingName.isNotBlank() else options.any { it.id == pendingId }
            cells.forEach { styleParentCell(it, !newParent && pendingId == it.category.id) }
        }
        fun chooseNewParent() {
            newParent = true; pendingId = null; refresh()
        }
        options.chunked(3).forEach { chunk ->
            val row = LinearLayout(requireContext())
            chunk.forEach { category ->
                val cell = parentCell(category) {
                    pendingId = category.id; newParent = false; pendingName = ""
                    newNameInput.setText(""); newNameInput.clearFocus()
                    ui.root.requestFocus()
                    ViewCompat.getWindowInsetsController(ui.root)?.hide(WindowInsetsCompat.Type.ime())
                    refresh()
                }
                cells += cell
                row.addView(cell.view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4))
                })
            }
            repeat(3 - chunk.size) { row.addView(View(requireContext()), LinearLayout.LayoutParams(0, 1, 1f)) }
            ui.body.addView(row)
        }
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_new_parent_label), 14f))
        val draftRow = LinearLayout(requireContext()).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        draftRow.addView(iconButton(pendingIcon).apply {
            id = R.id.button_record_category_parent_icon
            contentDescription = getString(R.string.record_category_parent_icon_hint)
            setOnClickListener {
                chooseNewParent()
                openIconPicker(pendingIcon) { selected -> pendingIcon = selected; setImageResource(iconResource(selected)) }
            }
        }, LinearLayout.LayoutParams(Ui.dp(requireContext(), 48), Ui.dp(requireContext(), 48)))
        newNameInput = input(pendingName, getString(R.string.record_category_parent_name_hint)) {
            pendingName = it
            if (it.isNotBlank()) { newParent = true; pendingId = null }
            refresh()
        }.apply {
            id = R.id.edit_record_category_parent_name
            setOnFocusChangeListener { _, focused -> if (focused) chooseNewParent() }
        }
        draftRow.addView(newNameInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Ui.dp(requireContext(), 12)
        })
        ui.body.addView(draftRow)
        ui.body.addView(Ui.text(requireContext(), getString(R.string.record_category_parent_draft_hint), 14f))
        refresh()
        ui.footer.addView(use)
        ui.footer.addView(Ui.button(requireContext(), getString(R.string.ledger_ui_back_adjust)) { choosing = false; render() })
    }

    private data class ParentCell(val view: LinearLayout, val image: ImageView, val label: TextView, val check: ImageView, val category: Category)

    private fun iconResource(value: String?) = value?.let { TablerIconCatalog.resourceId(it) }?.takeIf { it != 0 } ?: R.drawable.ic_category_other

    private fun iconButton(value: String) = androidx.appcompat.widget.AppCompatImageButton(requireContext()).apply {
        setImageResource(iconResource(value))
        contentDescription = getString(R.string.category_dialog_icon_hint)
        imageTintList = ColorStateList.valueOf(Ui.color(context, com.google.android.material.R.attr.colorOnSurface))
        setBackgroundResource(R.drawable.bg_input_surface)
        setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12))
    }

    private fun parentCell(category: Category, click: () -> Unit): ParentCell {
        val context = requireContext()
        val cell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            minimumHeight = Ui.dp(context, 88)
            setPadding(Ui.dp(context, 4), Ui.dp(context, 8), Ui.dp(context, 4), Ui.dp(context, 8))
            isClickable = true; isFocusable = true; contentDescription = category.name
            val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = attrs.getDrawable(0); attrs.recycle()
            setOnClickListener { click() }
        }
        val frame = android.widget.FrameLayout(context)
        val image = ImageView(context).apply { setImageResource(iconResource(category.icon)) }
        frame.addView(image, android.widget.FrameLayout.LayoutParams(Ui.dp(context, 28), Ui.dp(context, 28), android.view.Gravity.CENTER))
        val check = ImageView(context).apply { setImageResource(R.drawable.ic_check) }
        frame.addView(check, android.widget.FrameLayout.LayoutParams(Ui.dp(context, 16), Ui.dp(context, 16), android.view.Gravity.TOP or android.view.Gravity.END))
        cell.addView(frame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 32)))
        val label = TextView(context).apply {
            text = category.name; textSize = 14f; gravity = android.view.Gravity.CENTER
            maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        cell.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.dp(context, 4) })
        return ParentCell(cell, image, label, check, category)
    }

    private fun styleParentCell(cell: ParentCell, selected: Boolean) {
        val context = requireContext()
        val ink = Ui.color(context, if (selected) com.google.android.material.R.attr.colorOnPrimaryContainer else com.google.android.material.R.attr.colorOnSurface)
        cell.image.imageTintList = ColorStateList.valueOf(ink)
        cell.check.imageTintList = ColorStateList.valueOf(ink)
        cell.label.setTextColor(ink)
        cell.check.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        cell.view.isSelected = selected
        ViewCompat.setStateDescription(cell.view, getString(if (selected) R.string.ledger_ui_checked else R.string.ledger_ui_unchecked))
        cell.view.background = GradientDrawable().apply {
            setColor(Ui.color(context, if (selected) com.google.android.material.R.attr.colorPrimaryContainer else com.google.android.material.R.attr.colorSurface))
            cornerRadius = Ui.dp(context, 10).toFloat()
        }
    }

    private fun cancelPendingIcon() {
        val root = content?.root
        pendingIconListener?.let { listener ->
            if (root?.viewTreeObserver?.isAlive == true) root.viewTreeObserver.removeOnPreDrawListener(listener)
        }
        pendingIconListener = null
    }

    /** Do not size a second sheet against a window that is still occupied by the IME. */
    private fun openIconPicker(initialIcon: String, onSelected: (String) -> Unit) {
        val ui = content ?: return
        if (picker?.isShowing == true) return
        cancelPendingIcon()
        val root = ui.root
        root.findFocus()?.clearFocus()
        root.isFocusableInTouchMode = true
        root.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(root.windowToken, 0)
        ViewCompat.getWindowInsetsController(root)?.hide(WindowInsetsCompat.Type.ime())
        var lastBounds: Triple<Int, Int, Int>? = null
        var stableFrames = 0
        val listener = ViewTreeObserver.OnPreDrawListener {
            if (content !== ui || !isAdded || !root.isAttachedToWindow) {
                cancelPendingIcon()
            } else {
                val frame = Rect().also(root::getWindowVisibleDisplayFrame)
                val insets = ViewCompat.getRootWindowInsets(root)
                val imeVisible = insets?.isVisible(WindowInsetsCompat.Type.ime())
                    ?: (root.rootView.height - frame.bottom > Ui.dp(root.context, 120))
                val bounds = Triple(frame.height(), root.rootView.height, root.height)
                stableFrames = if (!imeVisible && imeAnimations.isEmpty() && bounds == lastBounds) stableFrames + 1 else 0
                lastBounds = bounds
                if (stableFrames >= 2) {
                    cancelPendingIcon()
                    val child = IconPickerDialog.show(requireContext(), initialIcon) { selected ->
                        if (selected != null && content === ui && isAdded) onSelected(selected)
                    }
                    picker = child
                    configureIconPicker(child)
                    child.setOnDismissListener {
                        if (picker === child) picker = null
                        if (content === ui) {
                            root.clearFocus(); root.requestFocus()
                            ViewCompat.getWindowInsetsController(root)?.hide(WindowInsetsCompat.Type.ime())
                        }
                    }
                } else ViewCompat.postInvalidateOnAnimation(root)
            }
            true
        }
        pendingIconListener = listener
        root.viewTreeObserver.addOnPreDrawListener(listener)
        ViewCompat.postInvalidateOnAnimation(root)
    }

    /** Local sizing only: other callers of the shared icon browser retain their contracts. */
    private fun configureIconPicker(child: BottomSheetDialog) {
        val root = child.findViewById<LinearLayout>(R.id.root_icon_picker) ?: return
        root.isFocusableInTouchMode = true; root.requestFocus()
        root.background = GradientDrawable().apply {
            setColor(Ui.color(requireContext(), com.google.android.material.R.attr.colorSurface))
            val radius = Ui.dp(requireContext(), 24).toFloat()
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
        val browser = root.getChildAt(root.childCount - 1)
        browser.layoutParams = (browser.layoutParams as LinearLayout.LayoutParams).apply { height = 0; weight = 1f }
        ViewCompat.setOnApplyWindowInsetsListener(root) { target, insets ->
            target.setPadding(target.paddingLeft, target.paddingTop, target.paddingRight,
                insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
        fun resize() {
            val frame = Rect().also(root::getWindowVisibleDisplayFrame)
            val available = frame.height().takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            val height = (available * .75f).toInt()
            if (root.layoutParams.height != height) root.layoutParams = root.layoutParams.apply { this.height = height }
        }
        resize()
        val listener = ViewTreeObserver.OnGlobalLayoutListener { resize() }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        })
        Ui.configure(child, root)
        child.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun save() {
        if (!validDraft()) return
        saving = true
        try {
            val leaf = DatabaseHelper(requireContext()).use {
                it.addLeafCategoryForRecord(Category(name = name.trim(), type = type, icon = icon, parentId = parentId),
                    parentName.takeIf { parentId == null }, parentIcon)
            }
            savedId = leaf.id
            dismiss()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(requireContext(), R.string.record_category_quick_add_failed, Toast.LENGTH_LONG).show()
        } catch (_: IllegalStateException) {
            Toast.makeText(requireContext(), R.string.record_category_quick_add_failed, Toast.LENGTH_LONG).show()
        } catch (_: android.database.sqlite.SQLiteException) {
            Toast.makeText(requireContext(), R.string.record_category_quick_add_failed, Toast.LENGTH_LONG).show()
        } finally { saving = false }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", name); outState.putString("icon", icon)
        outState.putLong("parent", parentId ?: 0L); outState.putString("parent_name", parentName)
        outState.putString("parent_icon", parentIcon); outState.putString("pending_icon", pendingIcon)
        outState.putBoolean("choosing", choosing); outState.putLong("pending", pendingId ?: 0L)
        outState.putString("pending_name", pendingName); outState.putBoolean("new_parent", newParent)
        outState.putLong("saved_id", savedId)
    }

    override fun onStart() {
        super.onStart()
        content?.let { Ui.configure(dialog as BottomSheetDialog, it.root) }
        content?.root?.let { root ->
            ViewCompat.setWindowInsetsAnimationCallback(root, object : WindowInsetsAnimationCompat.Callback(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) imeAnimations += animation
                }
                override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat = insets
                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    imeAnimations -= animation
                }
            })
        }
        (dialog as? BottomSheetDialog)?.behavior?.isDraggable = false
    }

    override fun onDismiss(dialog: DialogInterface) {
        cancelPendingIcon()
        super.onDismiss(dialog)
        if (activity?.isChangingConfigurations == true) return
        parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt("type", type); putLong("id", savedId) })
    }

    override fun onStop() {
        cancelPendingIcon()
        imeAnimations.clear()
        super.onStop()
    }

    override fun onDestroyView() {
        cancelPendingIcon()
        content?.root?.let { ViewCompat.setWindowInsetsAnimationCallback(it, null) }
        imeAnimations.clear()
        picker?.dismiss(); picker = null; content = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "record_category_add_sheet"
        const val RESULT = "record_category_add_result"
        fun newInstance(type: Int, parentId: Long?) = RecordCategoryAddSheetFragment().apply {
            arguments = Bundle().apply { putInt("type", type); putLong("parent", parentId ?: 0L) }
        }
    }
}
