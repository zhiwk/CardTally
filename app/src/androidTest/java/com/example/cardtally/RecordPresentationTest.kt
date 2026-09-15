package com.example.cardtally

import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.model.Record
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordPresentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun recordRows_showCategoryAndNoteOnSeparateLines_andTransferRouteAsTitle() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(
                instrumentation.targetContext,
                R.style.Theme_CardTally_Light
            )
            val parent = FrameLayout(context)
            val view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
            val holder = DateGroupAdapter.RecordViewHolder(view)
            val listener = object : DateGroupAdapter.OnRecordActionListener {
                override fun onEdit(record: Record) = Unit
                override fun onDelete(record: Record) = Unit
                override fun onMultiSelectChanged(selectedCount: Int) = Unit
                override fun onDeleteSelected(records: List<Record>) = Unit
                override fun onEnterMultiSelectMode(record: Record) = Unit
                override fun onToggleMultiSelect(record: Record) = Unit
            }

            holder.bind(
                Record(
                    category = "餐饮",
                    categoryNameSnapshot = "早餐",
                    description = "和同事一起吃饭",
                    type = 0,
                    assetSource = "招商银行"
                ),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals("早餐", view.findViewById<TextView>(R.id.text_category).text.toString())
            assertEquals(
                Typeface.NORMAL,
                view.findViewById<TextView>(R.id.text_category).typeface.style
            )
            assertEquals("和同事一起吃饭", view.findViewById<TextView>(R.id.text_description).text.toString())
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_description).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_asset).visibility)

            holder.bind(
                Record(
                    category = "资产转资产",
                    categoryNameSnapshot = "不应显示",
                    description = null,
                    type = 2,
                    assetSource = "余额宝",
                    destinationAssetSource = "支付宝一般基金"
                ),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals(
                "余额宝 → 支付宝一般基金",
                view.findViewById<TextView>(R.id.text_category).text.toString()
            )
            assertEquals(
                Typeface.NORMAL,
                view.findViewById<TextView>(R.id.text_category).typeface.style
            )
            assertEquals(View.GONE, view.findViewById<View>(R.id.text_description).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.text_asset).visibility)

            holder.bind(
                Record(category = "工资", description = "本月工资", type = 1),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals("工资", view.findViewById<TextView>(R.id.text_category).text.toString())
            assertEquals("本月工资", view.findViewById<TextView>(R.id.text_description).text.toString())
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_description).visibility)
        }
    }
}
