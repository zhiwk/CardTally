package com.example.cardtally

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.testing.IsolatedTestGuard
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryEditorSheetTest {
    private lateinit var context: Context
    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
    }
    @After fun tearDown() { if (::context.isInitialized) context.deleteDatabase("CardTally.db") }

    @Test fun editDraftSurvivesRecreationWithoutWritingOrResettingOriginalAppearance() {
        val id = DatabaseHelper(context).use { it.addCategory(Category(name = "编辑测试", icon = "tabler_shirt", color = "#123456")) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val editor = CategoryEditorSheetFragment.newInstance(0, null, id)
                editor.showNow(activity.supportFragmentManager, CategoryEditorSheetFragment.TAG)
                assertTrue(editor.requireDialog() is BottomSheetDialog)
                editor.requireView().findViewById<EditText>(R.id.edit_category_name).setText("未保存草稿")
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val editor = activity.supportFragmentManager.findFragmentByTag(CategoryEditorSheetFragment.TAG) as CategoryEditorSheetFragment
                assertEquals("未保存草稿", editor.requireView().findViewById<EditText>(R.id.edit_category_name).text.toString())
                DatabaseHelper(activity).use { db ->
                    val original = db.getCategoryById(id)!!
                    assertEquals("编辑测试", original.name); assertEquals("tabler_shirt", original.icon); assertEquals("#123456", original.color)
                }
            }
        }
    }

    @Test fun sameEditorCreatesAChildAndKeepsMergeDeleteActionsHidden() {
        val parent = DatabaseHelper(context).use { it.addCategory(Category(name = "新增宿主")) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val editor = CategoryEditorSheetFragment.newInstance(0, parent)
                editor.showNow(activity.supportFragmentManager, CategoryEditorSheetFragment.TAG)
                val root = editor.requireView()
                fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
                    (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
                val texts = views(root).filterIsInstance<TextView>()
                assertTrue(texts.none { it.text.toString() == activity.getString(R.string.category_merge_action) })
                root.findViewById<EditText>(R.id.edit_category_name).setText("新增子分类")
                texts.first { it.text.toString() == activity.getString(R.string.category_save) }.performClick()
                DatabaseHelper(activity).use { db ->
                    val child = db.getAllCategories().single { it.name == "新增子分类" }
                    assertEquals(parent, child.parentId); assertEquals(0, child.type)
                }
            }
        }
    }
}
