package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategoryAdapter
import com.example.cardtally.database.CategoryMergeException
import com.example.cardtally.database.CategoryMergePlanner
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.CategorySheetUi as Ui
import com.example.cardtally.util.ThemeColorHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout

class CategoryManageFragment : Fragment() {
    companion object { const val CATEGORY_SAVED_RESULT = "category_saved" }
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var textEmpty: TextView
    private var adapter: CategoryAdapter? = null
    private var currentType = 0
    private val sheets = mutableSetOf<BottomSheetDialog>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_category_manage, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        currentType = savedInstanceState?.getInt("type") ?: currentType
        recyclerCategories = root.findViewById(R.id.recycler_categories)
        textEmpty = root.findViewById(R.id.text_empty)
        root.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        root.findViewById<View>(R.id.btn_add).setOnClickListener { editor(null, null) }
        parentFragmentManager.setFragmentResultListener(CATEGORY_SAVED_RESULT, viewLifecycleOwner) { _, _ -> loadCategories() }
        parentFragmentManager.setFragmentResultListener(CategoryEditorSheetFragment.ACTION, viewLifecycleOwner) { _, result ->
            val id = result.getLong("id")
            if (result.getString("action") == "merge") showMergeTargets(id)
            else databaseHelper.getCategoryById(id)?.let(::showDeleteSheet)
        }
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(list: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val a = from.bindingAdapterPosition
                val b = to.bindingAdapterPosition
                if (a == RecyclerView.NO_POSITION || b == RecyclerView.NO_POSITION) return false
                adapter?.moveParent(a, b)
                return true
            }
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun clearView(list: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(list, holder)
                adapter?.parentIdsInOrder()?.let(databaseHelper::updateCategorySortOrders)
            }
        }).attachToRecyclerView(recyclerCategories)
        val tabs = root.findViewById<TabLayout>(R.id.tab_layout)
        for (index in 0 until tabs.tabCount) tabs.getTabAt(index)?.let { tab ->
            tab.setCustomView(R.layout.item_category_type_tab)
            (tab.customView as? TextView)?.text = tab.text
        }
        fun style(tab: TabLayout.Tab?, selected: Boolean) {
            (tab?.customView as? TextView)?.apply {
                setTextColor(if (selected) Ui.color(context, com.google.android.material.R.attr.colorOnSurface)
                    else ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted))
                paint.isFakeBoldText = selected; invalidate()
            }
        }
        tabs.getTabAt(currentType)?.select()
        for (index in 0 until tabs.tabCount) style(tabs.getTabAt(index), index == currentType)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { currentType = tab?.position ?: 0; style(tab, true); loadCategories() }
            override fun onTabUnselected(tab: TabLayout.Tab?) { style(tab, false) }
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
        loadCategories()
        return root
    }
    override fun onResume() { super.onResume(); if (::databaseHelper.isInitialized) loadCategories() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putInt("type", currentType) }
    override fun onDestroyView() {
        sheets.toList().forEach { it.dismiss() }; sheets.clear()
        recyclerCategories.adapter = null
        if (::databaseHelper.isInitialized) databaseHelper.close()
        super.onDestroyView()
    }
    private fun loadCategories() {
        val rows = databaseHelper.getCategoryTreeByType(currentType)
        textEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        recyclerCategories.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        if (adapter == null) adapter = CategoryAdapter(rows, object : CategoryAdapter.OnCategoryActionListener {
            override fun onEdit(category: Category) = editor(category, null)
            override fun onDelete(category: Category) = showDeleteSheet(category)
            override fun onAddChild(parent: Category) = editor(null, parent.id)
            override fun onChildOrderChanged(children: List<Category>) = databaseHelper.updateCategorySortOrders(children.map { it.id })
        }) else adapter?.updateCategories(rows)
        if (recyclerCategories.adapter !== adapter) recyclerCategories.adapter = adapter
    }
    private fun editor(category: Category?, parentId: Long?) {
        if (parentFragmentManager.findFragmentByTag(CategoryEditorSheetFragment.TAG) == null)
            CategoryEditorSheetFragment.newInstance(currentType, parentId, category?.id ?: 0L)
                .show(parentFragmentManager, CategoryEditorSheetFragment.TAG)
    }
    private fun show(dialog: BottomSheetDialog, content: Ui.Content) {
        dialog.setContentView(content.root); sheets += dialog
        dialog.setOnDismissListener { sheets -= dialog }
        dialog.show(); Ui.configure(dialog, content.root)
    }
    private fun complete() {
        sheets.toList().forEach { it.dismiss() }
        (parentFragmentManager.findFragmentByTag(CategoryEditorSheetFragment.TAG) as? CategoryEditorSheetFragment)?.dismiss()
        loadCategories()
    }
    private fun error(error: CategoryMergeException) {
        Toast.makeText(requireContext(), when (error.reason) {
            CategoryMergeException.Reason.STALE -> R.string.category_merge_stale
            CategoryMergeException.Reason.NAME_CONFLICT -> R.string.category_merge_conflict
            else -> R.string.category_merge_invalid
        }, Toast.LENGTH_LONG).show()
    }
    private fun showMergeTargets(sourceId: Long) {
        val rows = databaseHelper.getAllCategories()
        val depth = CategoryHierarchySettingsHelper.getCategoryMaxDepth(requireContext())
        val targets = rows.filter {
            try { CategoryMergePlanner.prepare(rows, sourceId, it.id, depth); true }
            catch (_: CategoryMergeException) { false }
        }
        if (targets.isEmpty()) { Toast.makeText(requireContext(), R.string.category_merge_no_targets, Toast.LENGTH_LONG).show(); return }
        val labels = targets.map { CategoryMergePlanner.path(rows, it.id) }
        val dialog = BottomSheetDialog(requireContext())
        val content = Ui.listContent(requireContext(), getString(R.string.category_merge_choose_target)) { dialog.dismiss() }
        val search = EditText(requireContext()).apply {
            hint = getString(R.string.category_search_hint); setSingleLine(true); textSize = 16f
            setBackgroundResource(R.drawable.bg_input_surface); backgroundTintList = null
            setTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(Ui.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12))
            visibility = if (targets.size > 5) View.VISIBLE else View.GONE
        }
        content.body.addView(search)
        val empty = Ui.text(requireContext(), getString(R.string.default_asset_no_matches), 14f)
        content.body.addView(empty)
        val list = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = true
        }
        // Only the list shrinks when the window/IME limits the sheet; header and search stay fixed.
        content.body.addView(list, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            Ui.dp(requireContext(), (targets.size * 72).coerceAtMost(280)), 1f))
        var filtered = targets.indices.toList()
        val choices = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = filtered.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(
                Ui.text(parent.context, "").apply {
                    minHeight = Ui.dp(context, 56); gravity = android.view.Gravity.CENTER_VERTICAL
                    isFocusable = true; isClickable = true
                    val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                    foreground = attrs.getDrawable(0); attrs.recycle()
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val index = filtered[position]
                (holder.itemView as TextView).text = labels[index]
                holder.itemView.setOnClickListener { showMergeConfirmation(sourceId, targets[index].id) }
            }
        }
        list.adapter = choices
        fun refresh() {
            filtered = targets.indices.filter { labels[it].contains(search.text.toString().trim(), true) }
            empty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            list.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
            // Keep enough room for wrapping paths, without retaining a blank viewport for no results.
            list.layoutParams = list.layoutParams.apply { height = Ui.dp(requireContext(), (filtered.size * 72).coerceAtMost(280)) }
            choices.notifyDataSetChanged()
        }
        search.doAfterTextChanged { refresh() }; refresh(); show(dialog, content)
    }
    private fun showMergeConfirmation(sourceId: Long, targetId: Long) {
        val preview = try { databaseHelper.previewCategoryMerge(sourceId, targetId) }
        catch (e: CategoryMergeException) { error(e); return }
        catch (_: android.database.sqlite.SQLiteException) { Toast.makeText(requireContext(), R.string.category_merge_failed, Toast.LENGTH_LONG).show(); return }
        val plan = preview.plan
        fun before(id: Long) = CategoryMergePlanner.path(plan.original, id)
        fun after(id: Long) = CategoryMergePlanner.path(plan.result, id)
        val details = plan.aliases.map { (old, target) -> getString(R.string.category_merge_detail, before(old), after(target)) } +
            plan.moved.keys.map { getString(R.string.category_merge_move_detail, before(it), after(it)) }
        val dialog = BottomSheetDialog(requireContext())
        val content = Ui.content(requireContext(), getString(R.string.category_merge_confirm_title)) { dialog.dismiss() }
        content.body.addView(Ui.text(requireContext(), getString(R.string.category_merge_summary, before(sourceId), after(targetId), preview.records, preview.recurring, preview.undo)))
        val detailText = Ui.text(requireContext(), details.joinToString("\n"), 14f).apply { visibility = View.GONE }
        content.body.addView(Ui.button(requireContext(), getString(R.string.category_merge_toggle_details)) {
            detailText.visibility = if (detailText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        })
        content.body.addView(detailText)
        val confirm = Ui.button(requireContext(), getString(R.string.category_merge_confirm), primary = true) {}
        content.footer.addView(confirm)
        content.footer.addView(Ui.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() })
        show(dialog, content)
        confirm.setOnClickListener {
            confirm.isEnabled = false
            try { databaseHelper.mergeCategories(preview); complete(); Toast.makeText(requireContext(), R.string.category_merge_success, Toast.LENGTH_SHORT).show() }
            catch (e: CategoryMergeException) { dialog.dismiss(); error(e) }
            catch (_: android.database.sqlite.SQLiteException) { dialog.dismiss(); Toast.makeText(requireContext(), R.string.category_merge_failed, Toast.LENGTH_LONG).show() }
        }
    }
    private fun showDeleteSheet(category: Category) {
        val dialog = BottomSheetDialog(requireContext())
        val content = Ui.content(requireContext(), getString(R.string.category_delete_title)) { dialog.dismiss() }
        content.body.addView(Ui.text(requireContext(), getString(R.string.category_delete_message, databaseHelper.buildCategoryPathLabel(category.id) ?: category.name)))
        val confirm = Ui.button(requireContext(), getString(R.string.category_delete_title), primary = true, danger = true) {}
        content.footer.addView(confirm)
        content.footer.addView(Ui.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() })
        show(dialog, content)
        confirm.setOnClickListener {
            confirm.isEnabled = false
            try { databaseHelper.deleteCategory(category.id); complete(); Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show() }
            catch (e: DatabaseHelper.CategoryOperationException) {
                confirm.isEnabled = true
                Toast.makeText(requireContext(), if (e.error == DatabaseHelper.CategoryOperationError.HAS_CHILDREN) R.string.category_error_has_children
                    else R.string.category_error_in_use, Toast.LENGTH_LONG).show()
            } catch (_: android.database.sqlite.SQLiteException) {
                confirm.isEnabled = true; Toast.makeText(requireContext(), R.string.category_editor_save_failed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
