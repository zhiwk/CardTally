package com.example.cardtally.util

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Shared in-app amount keypad for the add/edit record and add/edit asset flows.
 *
 * [alwaysVisible] supports the quick record layout, where the keypad is pinned to
 * the bottom of the page instead of opening below the amount field.
 *
 * Multiple numeric fields (record amount and transfer fee) can share one keypad:
 * call [bindTarget] for each field and [selectTarget] to choose which buffer the
 * next key press edits.
 */
class AmountKeypadController(
    private val context: Context,
    private val amount: EditText,
    private val keypad: View,
    private val normalActions: View?,
    private val alwaysVisible: Boolean = false,
    private val onConfirm: () -> Unit,
    private val allowNegative: Boolean = false
) {
    private var target: EditText = amount
    private val boundTargets = mutableSetOf<EditText>()

    private val digitIds = mapOf(
        0 to com.example.cardtally.R.id.keypad_0,
        1 to com.example.cardtally.R.id.keypad_1,
        2 to com.example.cardtally.R.id.keypad_2,
        3 to com.example.cardtally.R.id.keypad_3,
        4 to com.example.cardtally.R.id.keypad_4,
        5 to com.example.cardtally.R.id.keypad_5,
        6 to com.example.cardtally.R.id.keypad_6,
        7 to com.example.cardtally.R.id.keypad_7,
        8 to com.example.cardtally.R.id.keypad_8,
        9 to com.example.cardtally.R.id.keypad_9
    )

    fun bind() {
        bindTarget(amount)
        amount.setOnFocusChangeListener { _, hasFocus -> if (hasFocus && !alwaysVisible) show() }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_hide)?.setOnClickListener {
            hide()
        }

        digitIds.forEach { (digit, id) ->
            keypad.findViewById<View>(id)?.setOnClickListener { insert(digit.toString()) }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_dot)?.setOnClickListener {
            if (!target.text.toString().substringAfterLast('+').substringAfterLast('-').contains('.')) {
                insert(".")
            }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_minus)?.setOnClickListener {
            insertOperator("-")
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_plus)?.setOnClickListener {
            insertOperator("+")
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_delete)?.setOnClickListener {
            val start = target.selectionStart.coerceAtLeast(0)
            val end = target.selectionEnd.coerceAtLeast(0)
            if (start != end) {
                target.text.delete(minOf(start, end), maxOf(start, end))
            } else if (start > 0) {
                target.text.delete(start - 1, start)
            }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_confirm)?.setOnClickListener {
            if (target.text.contains('+') || target.text.contains('-')) {
                if (evaluateExpression()) updateConfirmLabel()
            } else {
                hide()
                onConfirm()
            }
        }
        updateConfirmLabel()

        if (alwaysVisible) {
            activateTarget(amount)
            keypad.visibility = View.VISIBLE
        }
    }

    /** Registers another numeric field that shares this keypad. */
    fun bindTarget(editText: EditText) {
        if (!boundTargets.add(editText)) return
        editText.showSoftInputOnFocus = false
        editText.setOnClickListener { selectTarget(editText) }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (target === editText) updateConfirmLabel()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    /** Points subsequent key presses at [editText] and shows the keypad. */
    fun selectTarget(editText: EditText) {
        editText.rootView.findFocus()?.let { focusedView ->
            if (focusedView !== editText) focusedView.clearFocus()
        }
        boundTargets.forEach { boundTarget ->
            if (boundTarget !== editText) boundTarget.clearFocus()
        }
        activateTarget(editText)
        show()
    }

    fun currentTarget(): EditText = target

    /** Switches the active buffer without showing the keypad. */
    fun resetTarget(editText: EditText) {
        target = editText
        if (alwaysVisible) activateTarget(editText)
    }

    private fun insert(value: String) {
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(0)
        target.text.replace(minOf(start, end), maxOf(start, end), value)
        target.setSelection((minOf(start, end) + value.length).coerceAtMost(target.length()))
    }

    private fun insertOperator(operator: String) {
        val current = target.text.toString()
        if (current.isBlank()) {
            if (allowNegative && operator == "-") insert(operator)
            return
        }
        if (current.last() in charArrayOf('+', '-')) return
        insert(operator)
    }

    private fun evaluateExpression(): Boolean {
        val expression = target.text.toString().trim()
        if (!expression.contains('+') && !expression.contains('-')) return false
        val result = Money.evaluateYuanExpression(expression) ?: return false
        target.setText(Money.formatYuan(result))
        target.setSelection(target.length())
        return true
    }

    private fun updateConfirmLabel() {
        keypad.findViewById<TextView>(com.example.cardtally.R.id.keypad_confirm)?.text =
            context.getString(
                if (target.text.contains('+') || target.text.contains('-')) {
                    com.example.cardtally.R.string.record_keypad_equals
                } else {
                    com.example.cardtally.R.string.record_keypad_confirm
                }
            )
    }

    fun show() {
        val inputMethod = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        fun hideSystemKeyboard() {
            inputMethod.hideSoftInputFromWindow(target.windowToken, 0)
            ViewCompat.getWindowInsetsController(target)?.hide(WindowInsetsCompat.Type.ime())
        }
        hideSystemKeyboard()
        // The IME can finish its pending show animation after the click callback.
        target.post { hideSystemKeyboard() }
        target.postDelayed({ hideSystemKeyboard() }, 200)
        if (target.text.toString() == context.getString(com.example.cardtally.R.string.amount_default)) {
            target.setText("")
            target.setSelection(0)
        }
        keypad.visibility = View.VISIBLE
        normalActions?.visibility = View.GONE
    }

    fun hide() {
        keypad.visibility = View.GONE
        normalActions?.visibility = View.VISIBLE
        if (target.text.isNullOrBlank()) {
            target.setText(context.getString(com.example.cardtally.R.string.amount_default))
        }
        target.clearFocus()
    }

    /** Temporarily hides the pinned keypad while a modal surface is open. */
    fun hideForModal() {
        if (alwaysVisible) {
            target.isCursorVisible = false
            keypad.visibility = View.GONE
        } else {
            hide()
        }
    }

    /** Restores a pinned keypad after a modal surface is dismissed. */
    fun restoreAfterModal() {
        if (alwaysVisible) {
            activateTarget(target)
            keypad.visibility = View.VISIBLE
        }
    }

    /** Hides the pinned keypad while the soft keyboard (e.g. the note field) is used. */
    fun hideForSoftKeyboard() {
        if (alwaysVisible) {
            target.isCursorVisible = false
            keypad.visibility = View.GONE
        }
    }

    fun restoreAfterSoftKeyboard() {
        if (alwaysVisible) {
            activateTarget(target)
            keypad.visibility = View.VISIBLE
        }
    }

    private fun activateTarget(editText: EditText) {
        target = editText
        editText.isCursorVisible = true
        editText.requestFocus()
        editText.setSelection(editText.length())
    }
}
