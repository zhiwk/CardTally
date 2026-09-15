package com.example.cardtally

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import android.widget.TextView
import android.text.Spanned
import android.text.style.ReplacementSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.AssetAdapter
import com.example.cardtally.adapter.AssetAdapter.OnAssetActionListener
import com.example.cardtally.model.Asset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetExclusionBadgeTest {

    @Test
    fun exclusionBadge_isVisibleOnlyForAssetsExcludedFromTotals() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().targetContext,
                R.style.Theme_CardTally_Light
            )
            val parent = FrameLayout(context)
            val adapter = AssetAdapter(
                listOf(
                    Asset(name = "不计入账户", categoryLabel = "借出", includeInTotal = false),
                    Asset(name = "正常账户", categoryLabel = "现金", includeInTotal = true)
                ),
                NoOpAssetActionListener
            )
            val holder = adapter.onCreateViewHolder(parent, 0)

            adapter.onBindViewHolder(holder, 0)
            val excludedName = holder.itemView.findViewById<TextView>(R.id.text_asset_name).text
            assertEquals("不计入账户 不计入", excludedName.toString())
            assertEquals(1, (excludedName as Spanned).getSpans(0, excludedName.length, ReplacementSpan::class.java).size)
            assertEquals("借出", holder.itemView.findViewById<android.widget.TextView>(R.id.text_asset_type).text)

            adapter.onBindViewHolder(holder, 1)
            assertEquals("正常账户", holder.itemView.findViewById<TextView>(R.id.text_asset_name).text.toString())
            assertEquals("现金", holder.itemView.findViewById<android.widget.TextView>(R.id.text_asset_type).text)
        }
    }

    private object NoOpAssetActionListener : OnAssetActionListener {
        override fun onClick(asset: Asset) = Unit
        override fun onEdit(asset: Asset) = Unit
        override fun onDelete(asset: Asset) = Unit
        override fun onArchive(asset: Asset) = Unit
    }
}
