package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.google.android.material.bottomnavigation.BottomNavigationView

class ArchivedAssetsFragment : Fragment() {
    private lateinit var recyclerAssets: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var textTotalAmount: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: AssetAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_archived_assets, container, false)

        recyclerAssets = view.findViewById(R.id.recycler_assets)
        textEmpty = view.findViewById(R.id.text_empty)
        textTotalAmount = view.findViewById(R.id.text_total_amount)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerAssets.layoutManager = LinearLayoutManager(requireContext())

        loadAssets()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun loadAssets() {
        val assets = databaseHelper.getArchivedAssets()
        var total = 0.0
        for (asset in assets) {
            total += asset.amount
        }

        textTotalAmount.text = String.format("¥%.2f", total)

        if (assets.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerAssets.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerAssets.visibility = View.VISIBLE

            if (adapter == null) {
                adapter = AssetAdapter(assets, object : AssetAdapter.OnAssetActionListener {
                    override fun onClick(asset: Asset) {
                        val recordsFragment = AssetRecordsFragment.newInstance(asset.name, asset.amount, asset.type)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, recordsFragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    override fun onEdit(asset: Asset) {
                        showEditDialog(asset)
                    }

                    override fun onDelete(asset: Asset) {
                        showDeleteDialog(asset)
                    }

                    override fun onArchive(asset: Asset) {
                        databaseHelper.unarchiveAsset(asset.id)
                        Toast.makeText(requireContext(), "已恢复", Toast.LENGTH_SHORT).show()
                        loadAssets()
                    }
                })
                recyclerAssets.adapter = adapter
            } else {
                adapter?.updateAssets(assets)
            }
        }
    }

    private fun showEditDialog(asset: Asset) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("编辑资产")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_asset, null)
        builder.setView(view)

        val editName = view.findViewById<android.widget.EditText>(R.id.edit_asset_name)
        val editAmount = view.findViewById<android.widget.EditText>(R.id.edit_asset_amount)
        val radioGroupType = view.findViewById<android.widget.RadioGroup>(R.id.radio_group_asset_type)

        editName.setText(asset.name)
        editAmount.setText(asset.amount.toString())

        when (asset.type) {
            0 -> view.findViewById<android.widget.RadioButton>(R.id.radio_cash).isChecked = true
            1 -> view.findViewById<android.widget.RadioButton>(R.id.radio_bank).isChecked = true
            2 -> view.findViewById<android.widget.RadioButton>(R.id.radio_alipay).isChecked = true
            3 -> view.findViewById<android.widget.RadioButton>(R.id.radio_wechat).isChecked = true
            else -> view.findViewById<android.widget.RadioButton>(R.id.radio_other).isChecked = true
        }

        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            val amountStr = editAmount.text.toString().trim()
            val type = when (radioGroupType.checkedRadioButtonId) {
                R.id.radio_cash -> 0
                R.id.radio_bank -> 1
                R.id.radio_alipay -> 2
                R.id.radio_wechat -> 3
                else -> 4
            }

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入资产名称", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = try {
                amountStr.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            asset.name = name
            asset.amount = amount
            asset.type = type

            val rowsAffected = databaseHelper.updateAsset(asset)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                loadAssets()
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showDeleteDialog(asset: Asset) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除资产")
            .setMessage("确定要删除\"${asset.name}\"吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.deleteAsset(asset.id)
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                loadAssets()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
}
