package com.example.cardtally.util

import com.example.cardtally.R

/** Single icon mapping shared by asset creation and asset list surfaces. */
object AssetTypeIconCatalog {
    private val iconsByLabel = mapOf(
        "现金" to R.drawable.tabler_cash,
        "储蓄卡" to R.drawable.tabler_building_bank,
        "支付宝" to R.drawable.ic_asset_brand_alipay,
        "微信钱包" to R.drawable.ic_asset_brand_wechat,
        "QQ钱包" to R.drawable.ic_asset_brand_qq,
        "京东" to R.drawable.ic_asset_brand_jd,
        "其他" to R.drawable.tabler_wallet,
        "信用卡" to R.drawable.tabler_credit_card,
        "花呗" to R.drawable.ic_asset_brand_huabei,
        "白条" to R.drawable.ic_asset_brand_baitiao,
        "借呗" to R.drawable.ic_asset_brand_jiebei,
        "其他信用" to R.drawable.tabler_credit_card,
        "交通卡" to R.drawable.tabler_bus,
        "饭卡" to R.drawable.tabler_id,
        "话费" to R.drawable.tabler_wallet,
        "会员卡" to R.drawable.tabler_id,
        "押金" to R.drawable.tabler_pig_money,
        "其他充值卡" to R.drawable.tabler_wallet,
        "股票" to R.drawable.tabler_trending_up,
        "基金" to R.drawable.tabler_building_bank,
        "黄金" to R.drawable.tabler_currency_yuan,
        "其他理财" to R.drawable.tabler_pig_money,
        "借出" to R.drawable.tabler_cash,
        "其他应收" to R.drawable.tabler_receipt,
        "借入" to R.drawable.tabler_cash,
        "其他应付" to R.drawable.tabler_receipt
    )

    fun isBrandIcon(resource: Int): Boolean = BankIconCatalog.isBankIcon(resource) || resource in setOf(
        R.drawable.ic_asset_brand_alipay,
        R.drawable.ic_asset_brand_wechat,
        R.drawable.ic_asset_brand_qq,
        R.drawable.ic_asset_brand_jd,
        R.drawable.ic_asset_brand_huabei,
        R.drawable.ic_asset_brand_baitiao,
        R.drawable.ic_asset_brand_jiebei
    )

    /** Brand badges keep their own colors and fill the existing circular icon container. */
    fun bindIcon(view: android.widget.ImageView, resource: Int, normalSizeDp: Int, tint: Int) {
        val brand = isBrandIcon(resource)
        val size = if (brand) android.view.ViewGroup.LayoutParams.MATCH_PARENT
            else (normalSizeDp * view.resources.displayMetrics.density).toInt()
        view.layoutParams = view.layoutParams.apply { width = size; height = size }
        androidx.core.widget.ImageViewCompat.setImageTintList(view, null)
        view.imageTintList = null
        view.clearColorFilter()
        if (BankIconCatalog.isBankIcon(resource)) {
            view.setImageDrawable(BankIconDrawable.create(view.context, resource))
        } else {
            view.setImageResource(resource)
        }
        if (!brand) view.setColorFilter(tint)
    }

    fun resourceForLabel(label: String): Int? = iconsByLabel[label]
}
