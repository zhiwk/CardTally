package com.example.cardtally

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private val showLedgers: Boolean
        get() = arguments?.getString(ARG_MODE) != MODE_ASSETS

    private val showAssets: Boolean
        get() = arguments?.getString(ARG_MODE) != MODE_LEDGERS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ledger_management, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        list = view.findViewById(R.id.ledger_management_list)
        assetList = view.findViewById(R.id.ledger_management_asset_list)
        view.findViewById<TextView>(R.id.ledger_management_hint).visibility = if (showLedgers) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.ledger_management_title).setText(
            if (showLedgers) R.string.ledger_management_ledgers_page_title else R.string.ledger_management_assets_page_title
        )
        val actionButton = view.findViewById<ImageView>(R.id.button_ledger_management_add)
        actionButton.visibility = View.VISIBLE
        actionButton.setImageResource(if (showLedgers) R.drawable.ic_add else R.drawable.tabler_list_check)
        actionButton.contentDescription = getString(if (showLedgers) R.string.ledger_management_add else R.string.ledger_management_select)
        view.findViewById<View>(R.id.button_ledger_management_back).setOnClickListener { parentFragmentManager.popBackStack() }
        actionButton.setOnClickListener {
            if (showLedgers) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerSetupFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                selectionMode = !selectionMode
                selectedLedgerIds.clear()
                actionButton.setImageResource(
                    if (selectionMode) R.drawable.ic_close else R.drawable.tabler_list_check
                )
                actionButton.contentDescription = getString(
                    if (selectionMode) R.string.dialog_cancel else R.string.ledger_management_select
                )
                render()
            }
        }
        view.findViewById<View>(R.id.button_ledger_management_delete_selected).setOnClickListener { deleteSelectedLedgers() }
        view.findViewById<View>(R.id.button_ledger_management_merge_selected).setOnClickListener { mergeSelectedLedgers() }
        render()
        return view
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized && isAdded) render()
    }

    private fun render() {
        list.removeAllViews()
        list.visibility = if (showLedgers) View.VISIBLE else View.GONE
        assetList.visibility = if (showAssets) View.VISIBLE else View.GONE
        viewSelectionActions()
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

    private fun viewSelectionActions() {
        if (!::assetList.isInitialized || !isAdded) return
        val actions = view?.findViewById<View>(R.id.ledger_management_selection_actions) ?: return
        actions.visibility = if (showAssets && selectionMode) View.VISIBLE else View.GONE
    }

    private fun renderAssets() {
        assetList.removeAllViews()
        val ledgers = databaseHelper.getLedgers()
         val currentId = LedgerSession.getCurrentId(requireContext())
        fun resolvePoolId(ledgerId: Long): Long {
            val visited = mutableSetOf<Long>()
            var poolId = ledgerId
            while (visited.add(poolId)) {
                val sourceId = databaseHelper.getSharedSourceLedgerId(poolId) ?: break
                poolId = sourceId
            }
            return poolId
        }
        ledgers.groupBy { ledger -> resolvePoolId(ledger.id) }
            .forEach { (poolId, poolLedgers) ->
                val summary = databaseHelper.getLedgerAssetPoolSummary(poolId)
                val poolTitle = if (poolLedgers.size > 1) {
                    getString(R.string.ledger_management_asset_pool_shared)
                } else {
                    getString(R.string.ledger_management_asset_pool_independent)
                }
                assetList.addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 8.dp, 0, 4.dp)
                    addView(TextView(requireContext()).apply {
                        text = poolTitle
                        textSize = 14f
                        setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_meta, summary.first, summary.second)
                        textSize = 12f
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
                        minimumHeight = 48.dp
                        setPadding(0, 4.dp, 0, 4.dp)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isClickable = true
                    setOnClickListener {
                        if (selectionMode) {
                            val selectedRoot = selectedLedgerIds.firstOrNull()?.let(::resolvePoolId)
                            if (selectedRoot != null && selectedRoot != poolId && ledger.id !in selectedLedgerIds) {
                                Toast.makeText(requireContext(), R.string.ledger_management_merge_same_group_required, Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            if (!selectedLedgerIds.add(ledger.id)) selectedLedgerIds.remove(ledger.id)
                            render()
                        } else {
                            LedgerSession.setCurrentId(requireContext(), ledger.id)
                            render()
                        }
                    }
                }
                    if (selectionMode) row.addView(RadioButton(requireContext()).apply {
                        isChecked = ledger.id in selectedLedgerIds
                        isClickable = false
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp).apply {
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
                            textSize = 14f
                            setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurface_light))
                        })
                        addView(TextView(requireContext()).apply {
                            text = ledger.subtitle
                            textSize = 12f
                            setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                        })
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    if (ledger.id == currentId) row.addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_current)
                        textSize = 12f
                        setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.onSurfaceVariant_light))
                    })
                    val rowParams = LinearLayout.LayoutParams(-1, -2).apply {
                        if (index < poolLedgers.lastIndex) bottomMargin = 2.dp
                    }
                    card.addView(row, rowParams)
                }
                assetList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8.dp })
            }
    }

    private fun deleteSelectedLedgers() {
        if (selectedLedgerIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.ledger_management_selection_required, Toast.LENGTH_SHORT).show()
            return
        }
        val ledgers = databaseHelper.getLedgers()
        if (selectedLedgerIds.size >= ledgers.size) {
            Toast.makeText(requireContext(), R.string.ledger_management_delete_all_forbidden, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ledger_management_delete_selected)
            .setMessage(getString(R.string.ledger_management_delete_message, selectedLedgerIds.size))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.ledger_management_delete_selected) { _, _ ->
                if (databaseHelper.deleteLedgers(selectedLedgerIds)) {
                    val newCurrent = databaseHelper.getLedgers().firstOrNull()?.id
                    if (newCurrent != null) LedgerSession.setCurrentId(requireContext(), newCurrent)
                    selectedLedgerIds.clear()
                    selectionMode = false
                    render()
                }
            }.show()
    }

    private fun mergeSelectedLedgers() {
        if (selectedLedgerIds.size != 2) {
            Toast.makeText(requireContext(), R.string.ledger_management_merge_two_required, Toast.LENGTH_SHORT).show()
            return
        }
        val selected = selectedLedgerIds.toList()
        val masterId = databaseHelper.getMasterLedgerId()
        val targets = selected.filter { it == masterId || masterId !in selected }
        val targetNames = targets.mapNotNull { id -> databaseHelper.getLedgers().firstOrNull { it.id == id }?.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ledger_management_merge_keep_title)
            .setSingleChoiceItems(targetNames, -1) { dialog, which ->
                val targetId = targets[which]
                val sourceId = selected.first { it != targetId }
                dialog.dismiss()
                confirmMerge(sourceId, targetId)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /** Explicit source → target confirmation; nothing is written before this. */
    private fun confirmMerge(sourceLedgerId: Long, targetLedgerId: Long) {
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
        val ledgers = databaseHelper.getLedgers()
        val source = ledgers.firstOrNull { it.id == sourceLedgerId } ?: return
        val target = ledgers.firstOrNull { it.id == targetLedgerId } ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ledger_management_merge_title)
            .setMessage(
                getString(
                    R.string.ledger_management_merge_confirm_message,
                    source.name,
                    target.name,
                    databaseHelper.getLedgerRecordCount(source.id)
                )
            )
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.ledger_management_merge_confirm) { _, _ ->
                if (databaseHelper.mergeLedgerInto(source.id, target.id)) {
                    // Only touch the active-ledger preference after a successful merge.
                    if (LedgerSession.getCurrentId(requireContext()) == source.id) {
                        LedgerSession.setCurrentId(requireContext(), target.id)
                    }
                    selectedLedgerIds.clear()
                    selectionMode = false
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_done, Toast.LENGTH_SHORT).show()
                    render()
                } else {
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_failed, Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun resolveLedgerIcon(ledger: Ledger): Int {
        return TablerIconCatalog.resourceId(ledger.iconName).takeIf { it != 0 } ?: R.drawable.ic_book
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
