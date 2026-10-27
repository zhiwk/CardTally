package com.example.cardtally

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatCheckBox
import android.widget.TextView
import android.widget.Toast
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.example.cardtally.util.CategorySheetUi
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Ledger
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.TablerIconCatalog

class LedgerManagementFragment : Fragment() {
    companion object {
        private const val ARG_MODE = "mode"
        private const val MODE_ASSETS = "assets"
        private const val MODE_LEDGERS = "ledgers"

        fun newLedgerSelectorInstance() = LedgerManagementFragment().apply {
            arguments = Bundle().apply { putString(ARG_MODE, MODE_LEDGERS) }
        }

        fun newAssetManagementInstance() = LedgerManagementFragment().apply {
            arguments = Bundle().apply { putString(ARG_MODE, MODE_ASSETS) }
        }
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var list: LinearLayout
    private lateinit var assetList: LinearLayout
    private var selectionMode = false
    private val selectedLedgerIds = mutableSetOf<Long>()
    private var activeSheet: BottomSheetDialog? = null

    private val showLedgers: Boolean
        get() = arguments?.getString(ARG_MODE) != MODE_ASSETS

    private val showAssets: Boolean
        get() = arguments?.getString(ARG_MODE) != MODE_LEDGERS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ledger_management, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        selectionMode = savedInstanceState?.getBoolean("selection_mode") ?: false
        selectedLedgerIds.clear()
        savedInstanceState?.getLongArray("selected_ledgers")?.let { selectedLedgerIds.addAll(it.toList()) }
        list = view.findViewById(R.id.ledger_management_list)
        assetList = view.findViewById(R.id.ledger_management_asset_list)
        view.findViewById<TextView>(R.id.ledger_management_hint).visibility = if (showLedgers) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.ledger_management_title).setText(
            if (showLedgers) R.string.ledger_management_ledgers_page_title else R.string.ledger_management_assets_page_title
        )
        val actionButton = view.findViewById<ImageView>(R.id.button_ledger_management_add)
        actionButton.visibility = if (showLedgers) View.VISIBLE else View.GONE
        actionButton.setImageResource(if (showLedgers) R.drawable.ic_add else R.drawable.tabler_list_check)
        actionButton.contentDescription = getString(if (showLedgers) R.string.ledger_management_add else R.string.ledger_management_select)
        view.findViewById<View>(R.id.button_ledger_management_back).setOnClickListener { parentFragmentManager.popBackStack() }
        actionButton.setOnClickListener {
            if (showLedgers) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerSetupFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        view.findViewById<TextView>(R.id.button_ledger_management_manage).apply {
            visibility = if (showAssets && !showLedgers) View.VISIBLE else View.GONE
            setOnClickListener {
                selectionMode = !selectionMode
                selectedLedgerIds.clear()
                render()
            }
        }
        view.findViewById<View>(R.id.button_ledger_management_delete_selected).setOnClickListener { deleteSelectedLedgers() }
        view.findViewById<View>(R.id.button_ledger_management_merge_selected).setOnClickListener { mergeSelectedLedgers() }
        render(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized && isAdded) render()
    }

    private fun render(root: View? = view) {
        list.removeAllViews()
        list.visibility = if (showLedgers) View.VISIBLE else View.GONE
        assetList.visibility = if (showAssets) View.VISIBLE else View.GONE
        selectedLedgerIds.retainAll(databaseHelper.getLedgers().map { it.id }.toSet())
        viewSelectionActions(root)
        if (!showLedgers) {
            renderAssets()
            return
        }
        val currentId = LedgerSession.getCurrentId(requireContext()) ?: databaseHelper.getLedgers().firstOrNull()?.id
        databaseHelper.getLedgers().forEach { ledger ->
                val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12.dp, 10.dp, 12.dp, 10.dp)
                background = GradientDrawable().apply { setColor(ThemeColorHelper.resolveCardSurface(requireContext())); cornerRadius = 12.dp.toFloat() }
                isClickable = true
                setOnClickListener {
                    LedgerSession.setCurrentId(requireContext(), ledger.id)
                    parentFragmentManager.popBackStack()
                }
            }
            val icon = ImageView(requireContext()).apply {
                setImageResource(resolveLedgerIcon(ledger))
                setColorFilter(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
            }
            row.addView(icon)
            val textColumn = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(12.dp, 0, 0, 0) }
            textColumn.addView(TextView(requireContext()).apply { text = ledger.name; textSize = 16f; setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurface_light)) })
            textColumn.addView(TextView(requireContext()).apply { text = ledger.subtitle; textSize = 12f; setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light)) })
            row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(requireContext()).apply {
                text = getString(R.string.ledger_management_count, databaseHelper.getLedgerRecordCount(ledger.id))
                textSize = 12f; setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light)); gravity = Gravity.CENTER_VERTICAL
            })
            row.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_edit)
                setColorFilter(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                contentDescription = getString(R.string.ledger_management_edit)
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp)
                setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LedgerSetupFragment.newEditInstance(ledger.id))
                        .addToBackStack(null)
                        .commit()
                }
            })
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8.dp })
            if (ledger.id == currentId) row.alpha = 1f
        }
        if (showAssets) renderAssets()
    }

    private fun viewSelectionActions(root: View?) {
        if (!::assetList.isInitialized || !isAdded) return
        val actions = root?.findViewById<View>(R.id.ledger_management_selection_actions) ?: return
        actions.visibility = if (showAssets && selectionMode) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.button_ledger_management_manage).setText(
            if (selectionMode) R.string.dialog_cancel else R.string.ledger_ui_manage)
        root.findViewById<View>(R.id.ledger_management_selection_header).visibility = actions.visibility
        root.findViewById<TextView>(R.id.ledger_management_selection_count).text =
            getString(R.string.ledger_ui_selected_count, selectedLedgerIds.size)
        val masterSelected = databaseHelper.getMasterLedgerId() in selectedLedgerIds
        root.findViewById<TextView>(R.id.ledger_management_selection_reason).setText(when {
            masterSelected -> R.string.ledger_ui_master_hint
            else -> R.string.ledger_ui_selection_hint
        })
        root.findViewById<MaterialButton>(R.id.button_ledger_management_delete_selected).apply {
            isEnabled = selectedLedgerIds.isNotEmpty() && !masterSelected && selectedLedgerIds.size < databaseHelper.getLedgers().size
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_container))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.error_primary))
            alpha = if (isEnabled) 1f else .4f
        }
        root.findViewById<MaterialButton>(R.id.button_ledger_management_merge_selected).apply {
            isEnabled = selectedLedgerIds.size >= 2 && selectedLedgerIds.map(::resolvePoolId).distinct().size == 1
        }
    }

    private fun resolvePoolId(ledgerId: Long): Long {
        val visited = mutableSetOf<Long>()
        var poolId = ledgerId
        while (visited.add(poolId)) poolId = databaseHelper.getSharedSourceLedgerId(poolId) ?: break
        return poolId
    }

    private fun renderAssets() {
        assetList.removeAllViews()
        val ledgers = databaseHelper.getLedgers()
         val currentId = LedgerSession.getCurrentId(requireContext())
        ledgers.groupBy { ledger -> resolvePoolId(ledger.id) }
            .forEach { (poolId, poolLedgers) ->
                val summary = databaseHelper.getLedgerAssetPoolSummary(poolId)
                val poolTitle = if (poolLedgers.size > 1) {
                    getString(R.string.ledger_management_asset_pool_shared)
                } else {
                    getString(R.string.ledger_management_asset_pool_independent)
                }
                assetList.addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 8.dp, 0, 4.dp)
                    addView(TextView(requireContext()).apply {
                        text = poolTitle
                        textSize = 14f
                        setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        layoutParams = LinearLayout.LayoutParams(-1, -2)
                    })
                    addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_meta, summary.first, summary.second)
                        textSize = 14f
                        setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                    })
                }, LinearLayout.LayoutParams(-1, -2))
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply { setColor(ThemeColorHelper.resolveCardSurface(requireContext())); cornerRadius = 12.dp.toFloat() }
                    setPadding(12.dp, 8.dp, 12.dp, 8.dp)
                }
                poolLedgers.forEachIndexed { index, ledger ->
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        minimumHeight = 64.dp
                        setPadding(0, 4.dp, 0, 4.dp)
                        background = GradientDrawable().apply {
                            setColor(if (ledger.id in selectedLedgerIds) ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.surface_container_high)
                                else android.graphics.Color.TRANSPARENT)
                            cornerRadius = 8.dp.toFloat()
                        }
                        val selectedRoot = selectedLedgerIds.firstOrNull()?.let(::resolvePoolId)
                        val available = !selectionMode || selectedRoot == null || selectedRoot == poolId
                        alpha = if (available) 1f else .4f
                        isClickable = true
                        isFocusable = true
                        isEnabled = available
                        background = RippleDrawable(ColorStateList.valueOf(
                            ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.surface_container_high)), background,
                            GradientDrawable().apply { setColor(android.graphics.Color.WHITE); cornerRadius = 8.dp.toFloat() })
                        setOnClickListener {
                            if (selectionMode) {
                                val selectedRoot = selectedLedgerIds.firstOrNull()?.let(::resolvePoolId)
                                if (selectedRoot != null && selectedRoot != poolId && ledger.id !in selectedLedgerIds) {
                                    return@setOnClickListener
                                }
                                if (!selectedLedgerIds.add(ledger.id)) selectedLedgerIds.remove(ledger.id)
                                render()
                            } else {
                                showLedgerActions(ledger.id)
                            }
                        }
                    }
                    if (selectionMode) row.addView(AppCompatCheckBox(requireContext()).apply {
                        isChecked = ledger.id in selectedLedgerIds
                        isClickable = false
                        isFocusable = false
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                            gravity = Gravity.CENTER_VERTICAL
                        }
                    })
                    row.addView(ImageView(requireContext()).apply {
                        setImageResource(resolveLedgerIcon(ledger))
                        setColorFilter(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
                    })
                    row.addView(LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(12.dp, 0, 0, 0)
                        addView(TextView(requireContext()).apply {
                            text = ledger.name
                            textSize = 16f
                            setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurface_light))
                        })
                        addView(TextView(requireContext()).apply {
                            text = ledger.subtitle
                            textSize = 14f
                            setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        })
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    if (ledger.id == currentId) row.addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_current)
                        textSize = 12f
                        setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        setPadding(6.dp, 4.dp, 6.dp, 4.dp)
                        background = GradientDrawable().apply {
                            setColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.surface_container_high))
                            cornerRadius = 4.dp.toFloat()
                        }
                    })
                    if (!selectionMode) row.addView(ImageView(requireContext()).apply {
                        setImageResource(R.drawable.ic_arrow_right)
                        setColorFilter(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(20.dp, 20.dp).apply { marginStart = 8.dp })
                    row.contentDescription = listOfNotNull(ledger.name, ledger.subtitle,
                        if (ledger.id == currentId) getString(R.string.ledger_management_asset_pool_current) else null,
                        if (selectionMode) getString(if (ledger.id in selectedLedgerIds) R.string.ledger_ui_checked else R.string.ledger_ui_unchecked) else null
                    ).joinToString(", ")
                    val rowParams = LinearLayout.LayoutParams(-1, -2).apply {
                        if (index < poolLedgers.lastIndex) bottomMargin = 2.dp
                    }
                    card.addView(row, rowParams)
                }
                assetList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8.dp })
            }
    }

    private fun showLedgerActions(ledgerId: Long) {
        val ledger = databaseHelper.getLedgers().firstOrNull { it.id == ledgerId } ?: return
        val isMaster = ledgerId == databaseHelper.getMasterLedgerId()
        val hasTargets = mergeTargets(ledgerId).isNotEmpty()
        showSheet(ledger.name) { content, dialog ->
            content.body.addView(sheetText(getString(R.string.ledger_ui_action_hint)))
            content.body.addView(CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_merge_into), navigation = true) {
                dialog.dismiss()
                showMergeTargets(ledgerId)
            }.apply {
                isEnabled = !isMaster && hasTargets
                alpha = if (isEnabled) 1f else .4f
            })
            if (isMaster || !hasTargets) {
                content.body.addView(sheetText(getString(if (isMaster) R.string.ledger_management_merge_master_source else R.string.ledger_ui_no_merge_target)))
            }
            content.body.addView(CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_delete_ledger), danger = true) {
                dialog.dismiss()
                confirmDeleteLedgers(setOf(ledgerId))
            }.apply {
                isEnabled = !isMaster
                // Dim the existing semantic red against the sheet rather than replacing it with gray.
                alpha = if (isEnabled) 1f else .4f
            })
            content.footer.addView(CategorySheetUi.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() })
        }
    }

    private fun mergeTargets(sourceId: Long): List<Ledger> = databaseHelper.getLedgers().filter {
        databaseHelper.validateLedgerMerge(sourceId, it.id) == DatabaseHelper.LedgerMergeFailure.NONE
    }

    private fun showMergeTargets(sourceId: Long) {
        val source = databaseHelper.getLedgers().firstOrNull { it.id == sourceId } ?: return
        val targets = mergeTargets(sourceId)
        showSheet(getString(R.string.ledger_ui_choose_target)) { content, dialog ->
            content.body.addView(sheetText(getString(R.string.ledger_ui_source, source.name), 16f))
            content.body.addView(sheetText(getString(R.string.ledger_ui_target_hint)))
            if (targets.isEmpty()) content.body.addView(sheetText(getString(R.string.ledger_ui_no_merge_target)))
            targets.forEach { target ->
                content.body.addView(CategorySheetUi.button(requireContext(), target.name, navigation = true) {
                    dialog.dismiss()
                    confirmMerge(sourceId, target.id) { showMergeTargets(sourceId) }
                })
            }
            content.footer.addView(CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_back_adjust)) {
                dialog.dismiss()
                showLedgerActions(sourceId)
            })
        }
    }

    private fun deleteSelectedLedgers() = confirmDeleteLedgers(selectedLedgerIds.toSet())

    private fun confirmDeleteLedgers(deleting: Set<Long>) {
        if (deleting.isEmpty()) {
            Toast.makeText(requireContext(), R.string.ledger_management_selection_required, Toast.LENGTH_SHORT).show()
            return
        }
        val ledgers = databaseHelper.getLedgers()
        if (deleting.size >= ledgers.size) {
            Toast.makeText(requireContext(), R.string.ledger_management_delete_all_forbidden, Toast.LENGTH_SHORT).show()
            return
        }
        if (databaseHelper.getMasterLedgerId() in deleting) return
        showSheet(getString(R.string.ledger_ui_delete_title)) { content, dialog ->
            content.body.addView(sheetText(getString(R.string.ledger_management_delete_message, deleting.size)))
            ledgers.filter { it.id in deleting }.forEach { ledger ->
                content.body.addView(sheetText(getString(R.string.ledger_ui_delete_item, ledger.name,
                    databaseHelper.getLedgerRecordCount(ledger.id)), 16f))
            }
            val removesPool = ledgers.groupBy { resolvePoolId(it.id) }.values.any { members -> members.all { it.id in deleting } }
            content.body.addView(sheetText(getString(if (removesPool) R.string.ledger_ui_delete_pool_warning else R.string.ledger_ui_delete_shared_warning)))
            content.body.addView(sheetText(getString(R.string.ledger_ui_delete_warning)))
            val confirm = CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_delete_confirm), primary = true, danger = true) {
                if (databaseHelper.deleteLedgers(deleting)) {
                    dialog.dismiss()
                    if (LedgerSession.getCurrentId(requireContext()) in deleting) {
                        val newCurrent = databaseHelper.getLedgers().firstOrNull()?.id
                        if (newCurrent != null) LedgerSession.setCurrentId(requireContext(), newCurrent)
                    }
                    selectedLedgerIds.clear()
                    selectionMode = false
                    render()
                } else {
                    Toast.makeText(requireContext(), R.string.ledger_ui_delete_failed, Toast.LENGTH_LONG).show()
                }
            }
            addSheetActions(content, CategorySheetUi.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() }, confirm)
        }
    }

    private fun mergeSelectedLedgers() {
        if (selectedLedgerIds.size < 2) {
            Toast.makeText(requireContext(), R.string.ledger_management_merge_two_required, Toast.LENGTH_SHORT).show()
            return
        }
        chooseBatchMergeTarget(selectedLedgerIds.toSet())
    }

    private fun chooseBatchMergeTarget(selected: Set<Long>) {
        val ledgers = databaseHelper.getLedgers()
        if (selected.size < 2 || !selected.all { id -> ledgers.any { it.id == id } }) return
        if (selected.map(::resolvePoolId).distinct().size != 1) {
            Toast.makeText(requireContext(), R.string.ledger_management_merge_cross_group, Toast.LENGTH_LONG).show()
            return
        }
        val masterId = databaseHelper.getMasterLedgerId()
        val targets = ledgers.filter { it.id in selected && (it.id == masterId || masterId !in selected) }
        showSheet(getString(R.string.ledger_management_merge_keep_title)) { content, dialog ->
            content.body.addView(sheetText(getString(if (masterId in selected) R.string.ledger_ui_master_hint else R.string.ledger_ui_keep_hint)))
            targets.forEach { target ->
                content.body.addView(CategorySheetUi.button(requireContext(), target.name, navigation = true) {
                    dialog.dismiss()
                    confirmMerges(selected - target.id, target.id) { chooseBatchMergeTarget(selected) }
                })
            }
            content.footer.addView(CategorySheetUi.button(requireContext(), getString(R.string.dialog_cancel)) { dialog.dismiss() })
        }
    }

    /** Explicit source → target confirmation; nothing is written before this. */
    private fun confirmMerge(sourceLedgerId: Long, targetLedgerId: Long, back: () -> Unit = { mergeSelectedLedgers() }) {
        confirmMerges(setOf(sourceLedgerId), targetLedgerId, back)
    }

    private fun confirmMerges(sourceLedgerIds: Set<Long>, targetLedgerId: Long, back: () -> Unit) {
        if (sourceLedgerIds.isEmpty()) return
        for (sourceLedgerId in sourceLedgerIds) {
            when (databaseHelper.validateLedgerMerge(sourceLedgerId, targetLedgerId)) {
                DatabaseHelper.LedgerMergeFailure.CROSS_GROUP -> {
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_cross_group, Toast.LENGTH_LONG).show()
                    return
                }
                DatabaseHelper.LedgerMergeFailure.MASTER_AS_SOURCE -> {
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_master_source, Toast.LENGTH_LONG).show()
                    return
                }
                DatabaseHelper.LedgerMergeFailure.SAME_LEDGER,
                DatabaseHelper.LedgerMergeFailure.MISSING_LEDGER -> return
                DatabaseHelper.LedgerMergeFailure.NONE -> Unit
            }
        }
        val ledgers = databaseHelper.getLedgers()
        val sources = ledgers.filter { it.id in sourceLedgerIds }
        if (sources.size != sourceLedgerIds.size) return
        val target = ledgers.firstOrNull { it.id == targetLedgerId } ?: return
        showSheet(getString(R.string.ledger_ui_merge_title)) { content, dialog ->
            content.body.addView(sheetText(getString(R.string.ledger_ui_merge_intro)))
            content.body.addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp, 8.dp, 16.dp, 8.dp)
                background = GradientDrawable().apply {
                    setColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.surface_container))
                    cornerRadius = 12.dp.toFloat()
                }
                sources.forEach { source ->
                    addView(sheetText(getString(R.string.ledger_ui_source, source.name), 16f))
                }
                addView(sheetText("↓", 16f))
                addView(sheetText(getString(R.string.ledger_ui_target, target.name), 16f))
            })
            content.body.addView(sheetText(getString(R.string.ledger_ui_record_impact, sources.sumOf { databaseHelper.getLedgerRecordCount(it.id).toLong() })))
            content.body.addView(sheetText(getString(R.string.ledger_ui_merge_warning)).apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_primary))
            })
            val confirm = CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_merge_confirm), primary = true) {
                if (databaseHelper.mergeLedgersInto(sourceLedgerIds, target.id)) {
                    dialog.dismiss()
                    // Only touch the active-ledger preference after a successful merge.
                    if (LedgerSession.getCurrentId(requireContext()) in sourceLedgerIds) {
                        LedgerSession.setCurrentId(requireContext(), target.id)
                    }
                    selectedLedgerIds.clear()
                    selectionMode = false
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_done, Toast.LENGTH_SHORT).show()
                    render()
                } else {
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_failed, Toast.LENGTH_LONG).show()
                }
            }
            val adjust = CategorySheetUi.button(requireContext(), getString(R.string.ledger_ui_back_adjust)) {
                dialog.dismiss()
                back()
            }
            addSheetActions(content, adjust, confirm)
        }
    }

    private fun sheetText(value: String, size: Float = 14f) = CategorySheetUi.text(requireContext(), value, size).apply {
        if (size == 14f) setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
    }

    private fun addSheetActions(content: CategorySheetUi.Content, secondary: MaterialButton, primary: MaterialButton) {
        val row = LinearLayout(requireContext())
        val stacked = resources.configuration.fontScale >= 1.3f || resources.configuration.screenWidthDp < 320
        row.orientation = if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        listOf(secondary, primary).forEachIndexed { index, button ->
            row.addView(button, LinearLayout.LayoutParams(
                if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else 0,
                ViewGroup.LayoutParams.WRAP_CONTENT, if (stacked) 0f else 1f
            ).apply { if (!stacked && index == 1) marginStart = 8.dp })
        }
        content.footer.addView(row)
    }

    private fun showSheet(title: String, bind: (CategorySheetUi.Content, BottomSheetDialog) -> Unit) {
        activeSheet?.dismiss()
        val dialog = BottomSheetDialog(requireContext())
        val content = CategorySheetUi.content(requireContext(), title) { dialog.dismiss() }
        bind(content, dialog)
        dialog.setContentView(content.root)
        dialog.setOnDismissListener { if (activeSheet === dialog) activeSheet = null }
        activeSheet = dialog
        dialog.show()
        CategorySheetUi.configure(dialog, content.root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("selection_mode", selectionMode)
        outState.putLongArray("selected_ledgers", selectedLedgerIds.toLongArray())
    }

    override fun onDestroyView() {
        activeSheet?.dismiss()
        activeSheet = null
        super.onDestroyView()
    }

    private fun resolveLedgerIcon(ledger: Ledger): Int {
        return TablerIconCatalog.resourceId(ledger.iconName).takeIf { it != 0 } ?: R.drawable.ic_book
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
