package com.example.cardtally.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.IconCategoryCatalog

/**
 * Wires the shared "left group rail + right icon grid" browser into a host view.
 * Used by both the inline add-category browser and the shared picker sheet so
 * the two never drift apart.
 *
 * The grid shows icons without visible text. If [initialSelected] is not part of
 * any curated group (an icon saved before the curated sets existed, or a ledger
 * icon), a synthetic "current icon" group is prepended so the old selection
 * stays visible and re-selectable instead of silently disappearing.
 */
class IconBrowserBinder(
    private val context: Context,
    private val root: View,
    private val labels: Map<String, String>,
    private val onSelected: (String) -> Unit
) {
    private val groupsContainer: LinearLayout = root.findViewById(R.id.icon_browser_groups)
    private val grid: RecyclerView = root.findViewById(R.id.icon_browser_grid)
    private val adapter = CategoryIconGridAdapter(labels) { icon -> handleSelected(icon) }

    private val groups = mutableListOf<BrowserGroup>()
    private var selectedGroupIndex = 0

    var selectedIcon: String? = null
        private set

    init {
        grid.layoutManager = GridLayoutManager(context, GRID_SPAN)
        grid.adapter = adapter
        grid.setHasFixedSize(true)
    }

    fun bind(initialSelected: String?) {
        selectedIcon = initialSelected
        groups.clear()

        val titles = context.resources.getStringArray(R.array.icon_group_titles)
        IconCategoryCatalog.groups.forEachIndexed { index, group ->
            groups += BrowserGroup(
                title = titles.getOrNull(index)?.takeIf { it.isNotBlank() } ?: group.title,
                icons = group.icons
            )
        }

        val curatedContainsSelection = IconCategoryCatalog.groups.any { it.icons.contains(initialSelected) }
        if (!curatedContainsSelection && !initialSelected.isNullOrBlank()) {
            groups.add(
                0,
                BrowserGroup(context.getString(R.string.icon_browser_current_group), listOf(initialSelected))
            )
        }

        selectedGroupIndex = groups.indexOfFirst { it.icons.contains(initialSelected) }.coerceAtLeast(0)
        renderGroupRail()
        renderGrid()
    }

    /** Reflects a selection made outside the grid (for example the picker sheet). */
    fun updateSelection(icon: String?) {
        selectedIcon = icon
        adapter.setSelected(icon)
    }

    private fun renderGroupRail() {
        groupsContainer.removeAllViews()
        groups.forEachIndexed { index, group ->
            val selected = index == selectedGroupIndex
            val groupView = TextView(context).apply {
                text = group.title
                textSize = 14f
                minHeight = 48.dp
                gravity = android.view.Gravity.CENTER_VERTICAL
                setTextColor(context.getColor(if (selected) R.color.onPrimaryContainer_light else R.color.onSurfaceVariant_light))
                setTypeface(typeface, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setPadding(8.dp, 0, 8.dp, 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isClickable = true
                isFocusable = true
                isSelected = selected
                contentDescription = group.title
                background = if (selected) context.getDrawable(R.drawable.bg_secondary_action) else null
                setOnClickListener {
                    if (selectedGroupIndex != index) {
                        selectedGroupIndex = index
                        renderGroupRail()
                        renderGrid()
                    }
                }
            }
            groupsContainer.addView(groupView)
        }
    }

    private fun renderGrid() {
        adapter.submit(groups.getOrNull(selectedGroupIndex)?.icons.orEmpty(), selectedIcon)
    }

    private fun handleSelected(icon: String) {
        selectedIcon = icon
        adapter.setSelected(icon)
        onSelected(icon)
    }

    private data class BrowserGroup(val title: String, val icons: List<String>)

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()

    companion object {
        const val GRID_SPAN = 4
    }
}
