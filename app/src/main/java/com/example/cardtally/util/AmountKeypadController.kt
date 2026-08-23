package com.example.cardtally.util

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import java.util.Locale

/** Shared in-app amount keypad for add and edit record flows. */
class AmountKeypadController(
    private val context: Context,
    private val amount: EditText,
    private val keypad: View,
    private val normalActions: View,
    private val onConfirm: () -> Unit
) {
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
        amount.showSoftInputOnFocus = false
        amount.setOnClickListener { show() }
        amount.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) show() }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_hide).setOnClickListener {
            hide()
        }
        amount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateConfirmLabel()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        digitIds.forEach { (digit, id) ->
            keypad.findViewById<View>(id).setOnClickListener { insert(digit.toString()) }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_dot).setOnClickListener {
            if (!amount.text.toString().substringAfterLast('+').substringAfterLast('-').contains('.')) {
                insert(".")
            }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_minus).setOnClickListener {
            insertOperator("-")
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_plus).setOnClickListener {
            insertOperator("+")
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_delete).setOnClickListener {
            val start = amount.selectionStart.coerceAtLeast(0)
            val end = amount.selectionEnd.coerceAtLeast(0)
            if (start != end) {
                amount.text.delete(minOf(start, end), maxOf(start, end))
            } else if (start > 0) {
                amount.text.delete(start - 1, start)
            }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_confirm).setOnClickListener {
            if (amount.text.contains('+') || amount.text.contains('-')) {
                if (evaluateExpression()) updateConfirmLabel()
            } else {
                hide()
                onConfirm()
            }
        }
        updateConfirmLabel()
    }

    private fun insert(value: String) {
        val start = amount.selectionStart.coerceAtLeast(0)
        val end = amount.selectionEnd.coerceAtLeast(0)
        amount.text.replace(minOf(start, end), maxOf(start, end), value)
        amount.setSelection((minOf(start, end) + value.length).coerceAtMost(amount.length()))
    }

    private fun insertOperator(operator: String) {
        val current = amount.text.toString()
        if (current.isBlank() || current.last() in charArrayOf('+', '-')) return
        insert(operator)
    }

    private fun evaluateExpression(): Boolean {
        val expression = amount.text.toString().trim()
        if (!expression.contains('+') && !expression.contains('-')) return false
        val tokens = expression.split(Regex("(?=[+-])|(?<=[+-])"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        var result = tokens.first().toDoubleOrNull() ?: return false
        var index = 1
        while (index + 1 < tokens.size) {
            val operand = tokens[index + 1].toDoubleOrNull() ?: return false
            result = when (tokens[index]) {
                "+" -> result + operand
                "-" -> result - operand
                else -> return false
            }
            index += 2
        }
        amount.setText(String.format(Locale.US, "%.2f", result))
        amount.setSelection(amount.length())
        return true
    }

    private fun updateConfirmLabel() {
        keypad.findViewById<TextView>(com.example.cardtally.R.id.keypad_confirm).text =
            if (amount.text.contains('+') || amount.text.contains('-')) "=" else "确定"
    }

    fun show() {
        val inputMethod = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethod.hideSoftInputFromWindow(amount.windowToken, 0)
        if (amount.text.toString() == "0.00") {
            amount.setText("")
            amount.setSelection(0)
        }
        keypad.visibility = View.VISIBLE
        normalActions.visibility = View.GONE
    }

    fun hide() {
        keypad.visibility = View.GONE
        normalActions.visibility = View.VISIBLE
        amount.clearFocus()
    }
}
