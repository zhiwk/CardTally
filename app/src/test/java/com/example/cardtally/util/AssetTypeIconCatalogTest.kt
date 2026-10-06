package com.example.cardtally.util

import com.example.cardtally.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetTypeIconCatalogTest {
    @Test
    fun brandedAssetLabelsUseTheRequestedAppIcons() {
        assertEquals(R.drawable.ic_asset_brand_alipay, AssetTypeIconCatalog.resourceForLabel("支付宝"))
        assertEquals(R.drawable.ic_asset_brand_huabei, AssetTypeIconCatalog.resourceForLabel("花呗"))
        assertEquals(R.drawable.ic_asset_brand_wechat, AssetTypeIconCatalog.resourceForLabel("微信钱包"))
        assertEquals(R.drawable.ic_asset_brand_jiebei, AssetTypeIconCatalog.resourceForLabel("借呗"))
        assertEquals(R.drawable.ic_asset_brand_qq, AssetTypeIconCatalog.resourceForLabel("QQ钱包"))
        assertEquals(R.drawable.ic_asset_brand_jd, AssetTypeIconCatalog.resourceForLabel("京东"))
        assertEquals(R.drawable.ic_asset_brand_baitiao, AssetTypeIconCatalog.resourceForLabel("白条"))
    }

    @Test
    fun combinedCreditLabelIsNoLongerASelectableType() {
        assertEquals(null, AssetTypeIconCatalog.resourceForLabel("借呗 / 其他信用"))
        assertEquals(R.drawable.tabler_credit_card, AssetTypeIconCatalog.resourceForLabel("其他信用"))
    }
}
