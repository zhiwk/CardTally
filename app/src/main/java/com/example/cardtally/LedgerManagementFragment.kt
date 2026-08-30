package com.example.cardtally

import android.graphics.Color
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
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.SwipeToEditDeleteHelper

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
        actionButton.setImageResource(if (showLedgers) R.drawable.ic_add else R.drawable.ic_check)
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
                background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 12.dp.toFloat() }
                isClickable = true
                setOnClickListener {
                    LedgerSession.setCurrentId(requireContext(), ledger.id)
                    parentFragmentManager.popBackStack()
                }
                setOnLongClickListener {
                    if (ledger.id == currentId) return@setOnLongClickListener false
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.ledger_management_merge_title)
                        .setMessage(getString(R.string.ledger_management_merge_message, ledger.name))
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.ledger_management_merge_confirm) { _, _ ->
                            if (databaseHelper.mergeLedgerIntoCurrent(ledger.id)) {
                                Toast.makeText(requireContext(), R.string.ledger_management_merge_done, Toast.LENGTH_SHORT).show()
                                render()
                            }
                        }.show()
                    true
                }
            }
            val icon = ImageView(requireContext()).apply {
                setImageResource(resolveLedgerIcon(ledger))
                setColorFilter(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
            }
            row.addView(icon)
            val textColumn = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(12.dp, 0, 0, 0) }
            textColumn.addView(TextView(requireContext()).apply { text = ledger.name; textSize = 16f; setTextColor(Color.rgb(23,24,26)) })
            textColumn.addView(TextView(requireContext()).apply { text = ledger.subtitle; textSize = 12f; setTextColor(Color.GRAY) })
            row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(requireContext()).apply {
                text = getString(R.string.ledger_management_count, databaseHelper.getLedgerRecordCount(ledger.id))
                textSize = 12f; setTextColor(Color.GRAY); gravity = Gravity.CENTER_VERTICAL
            })
            row.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_edit)
                setColorFilter(Color.DKGRAY)
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
                        setTextColor(Color.rgb(70, 72, 76))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_meta, summary.first, summary.second)
                        textSize = 12f
                        setTextColor(Color.GRAY)
                    })
                }, LinearLayout.LayoutParams(-1, -2))
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 12.dp.toFloat() }
                    setPadding(12.dp, 8.dp, 12.dp, 8.dp)
                }
                poolLedgers.forEachIndexed { index, ledger ->
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        minimumHeight = 48.dp
                        setPadding(0, 4.dp, 0, 4.dp)
                        setBackgroundColor(Color.WHITE)
                    isClickable = true
                    setOnClickListener {
                        if (selectionMode) {
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
                        setColorFilter(Color.DKGRAY)
                        layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp)
                    })
                    row.addView(LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(12.dp, 0, 0, 0)
                        addView(TextView(requireContext()).apply {
                            text = ledger.name
                            textSize = 14f
                            setTextColor(Color.rgb(23, 24, 26))
                        })
                        addView(TextView(requireContext()).apply {
                            text = ledger.subtitle
                            textSize = 12f
                            setTextColor(Color.GRAY)
                        })
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    if (ledger.id == currentId) row.addView(TextView(requireContext()).apply {
                        text = getString(R.string.ledger_management_asset_pool_current)
                        textSize = 12f
                        setTextColor(Color.GRAY)
                    })
                    val rowParams = LinearLayout.LayoutParams(-1, -2).apply {
                        if (index < poolLedgers.lastIndex) bottomMargin = 2.dp
                    }
                    if (poolLedgers.size > 1 && !selectionMode) {
                        val rowContainer = android.widget.FrameLayout(requireContext())
                        val actions = LinearLayout(requireContext()).apply {
                            gravity = Gravity.CENTER
                            setBackgroundColor(Color.TRANSPARENT)
                            addView(ImageButton(requireContext()).apply {
                                id = R.id.btn_fork
                                setImageResource(R.drawable.tabler_arrow_fork)
                                setColorFilter(Color.rgb(55, 90, 67))
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    setColor(Color.rgb(220, 239, 227))
                                }
                                contentDescription = "独立资产池"
                                setPadding(10.dp, 10.dp, 10.dp, 10.dp)
                                layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp)
                            })
                        }
                        rowContainer.addView(actions, android.widget.FrameLayout.LayoutParams(56.dp, -1).apply {
                            gravity = Gravity.END
                        })
                        rowContainer.addView(row, android.widget.FrameLayout.LayoutParams(-1, -2))
                        SwipeToEditDeleteHelper(
                            row,
                            actions,
                            onEdit = {},
                            onDelete = {},
                            onFork = {
                                if (databaseHelper.forkLedgerAssetPool(ledger.id)) {
                                    Toast.makeText(requireContext(), "已转为独立资产池", Toast.LENGTH_SHORT).show()
                                    render()
                                }
                            },
                            onClick = { row.performClick() }
                        )
                        card.addView(rowContainer, rowParams)
                    } else {
                        card.addView(row, rowParams)
                    }
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
        if (selectedLedgerIds.size < 2) {
            Toast.makeText(requireContext(), R.string.ledger_management_merge_selection_required, Toast.LENGTH_SHORT).show()
            return
        }
        val currentId = LedgerSession.getCurrentId(requireContext())
        val targetId = currentId?.takeIf { it in selectedLedgerIds } ?: selectedLedgerIds.first()
        val sources = selectedLedgerIds.filter { it != targetId }
        val selectedNames = selectedLedgerIds.mapNotNull { id ->
            databaseHelper.getLedgers().firstOrNull { it.id == id }?.name
        }
        val targetName = databaseHelper.getLedgers().firstOrNull { it.id == targetId }?.name.orEmpty()
        val sourceNames = sources.mapNotNull { id ->
            databaseHelper.getLedgers().firstOrNull { it.id == id }?.name
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ledger_management_merge_title)
            .setMessage(
                getString(
                    R.string.ledger_management_merge_selection_message,
                    selectedNames.joinToString("、"),
                    targetName
                ) + "\n" + sourceNames.joinToString("、")
            )
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.ledger_management_merge_confirm) { _, _ ->
                LedgerSession.setCurrentId(requireContext(), targetId)
                val merged = sources.all { databaseHelper.mergeLedgerIntoCurrent(it) }
                if (merged) {
                    Toast.makeText(requireContext(), R.string.ledger_management_merge_done, Toast.LENGTH_SHORT).show()
                    selectedLedgerIds.clear()
                    selectionMode = false
                    render()
                }
            }.show()
    }

    private fun resolveLedgerIcon(ledger: Ledger): Int {
        return TablerIconCatalog.resourceId(ledger.iconName).takeIf { it != 0 } ?: R.drawable.ic_book
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
