package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.state.AssetType
import com.example.cardtally.util.AssetTypeIconCatalog

class AssetTypeSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_asset_type_select, container, false)

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindType(view, R.id.row_cash, "现金", R.drawable.tabler_cash, AssetType.CASH)
        bindType(view, R.id.row_bank, "储蓄卡", R.drawable.tabler_building_bank, AssetType.BANK)
        bindType(view, R.id.row_alipay, "支付宝", R.drawable.tabler_brand_alipay, AssetType.ALIPAY)
        bindType(view, R.id.row_wechat, "微信钱包", R.drawable.tabler_brand_wechat, AssetType.WECHAT)
        bindType(view, R.id.row_qq, "QQ钱包", R.drawable.tabler_brand_qq, AssetType.ALIPAY)
        bindType(view, R.id.row_jd, "京东", R.drawable.ic_brand_jd, AssetType.ALIPAY)
        bindType(view, R.id.row_other_fund, "其他", R.drawable.tabler_wallet, AssetType.BANK)
        bindType(view, R.id.row_credit_card, "信用卡", R.drawable.tabler_credit_card, AssetType.BANK)
        bindType(view, R.id.row_huabei, "花呗", R.drawable.tabler_brand_alipay, AssetType.ALIPAY)
        bindType(view, R.id.row_baitiao, "白条", R.drawable.ic_brand_jd, AssetType.ALIPAY)
        bindType(view, R.id.row_jiebei, "借呗", R.drawable.tabler_brand_wechat, AssetType.BANK)
        bindType(view, R.id.row_credit_other, "其他信用", R.drawable.tabler_credit_card, AssetType.BANK)
        bindType(view, R.id.row_transport_card, "交通卡", R.drawable.tabler_bus, AssetType.BANK)
        bindType(view, R.id.row_meal_card, "饭卡", R.drawable.tabler_id, AssetType.BANK)
        bindType(view, R.id.row_phone_card, "话费", R.drawable.tabler_wallet, AssetType.BANK)
        bindType(view, R.id.row_membership_card, "会员卡", R.drawable.tabler_id, AssetType.BANK)
        bindType(view, R.id.row_deposit, "押金", R.drawable.tabler_pig_money, AssetType.BANK)
        bindType(view, R.id.row_recharge_other, "其他充值卡", R.drawable.tabler_wallet, AssetType.BANK)
        bindType(view, R.id.row_stock, "股票", R.drawable.tabler_trending_up, AssetType.BANK)
        bindType(view, R.id.row_fund, "基金", R.drawable.tabler_building_bank, AssetType.BANK)
        bindType(view, R.id.row_gold, "黄金", R.drawable.tabler_currency_yuan, AssetType.BANK)
        bindType(view, R.id.row_invest_other, "其他理财", R.drawable.tabler_pig_money, AssetType.BANK)
        bindType(view, R.id.row_lend, "借出", R.drawable.tabler_cash, AssetType.BANK)
        bindType(view, R.id.row_receivable_other, "其他应收", R.drawable.tabler_receipt, AssetType.BANK)
        bindType(view, R.id.row_borrow, "借入", R.drawable.tabler_cash, AssetType.BANK)
        bindType(view, R.id.row_payable_other, "其他应付", R.drawable.tabler_receipt, AssetType.BANK)
        return view
    }

    private fun bindType(view: View, rowId: Int, label: String, iconRes: Int, type: AssetType) {
        val row = view.findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.option_label).text = label
        val resolvedIcon = AssetTypeIconCatalog.resourceForLabel(label) ?: iconRes
        AssetTypeIconCatalog.bindIcon(
            row.findViewById(R.id.option_icon), resolvedIcon, 20,
            com.example.cardtally.util.ThemeColorHelper.resolveColor(
                requireContext(), com.google.android.material.R.attr.colorPrimary
            )
        )
        row.setOnClickListener {
            if (label == "储蓄卡" || label == "信用卡") {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, BankSelectFragment.newInstance(label))
                    .addToBackStack("bank_select")
                    .commit()
                return@setOnClickListener
            }
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AddAssetFragment.newInstance(
                        type,
                        label,
                         resources.getResourceEntryName(resolvedIcon)
                    )
                )
                .addToBackStack("asset_add")
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.VISIBLE
    }
}
