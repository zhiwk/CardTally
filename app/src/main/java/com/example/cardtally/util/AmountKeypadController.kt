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
enum class AmountKeypadCompletionMode {
    DISMISS_KEYPAD,
    SAVE_RECORD,
    SAVE_ASSET
}

class AmountKeypadController(
    private val context: Context,
    private val amount: EditText,
    private val keypad: View,
    private val normalActions: View?,
    private val alwaysVisible: Boolean = false,
    private val onConfirm: () -> Unit,
    private val allowNegative: Boolean = false,
    private val completionMode: AmountKeypadCompletionMode = AmountKeypadCompletionMode.DISMISS_KEYPAD,
    private val primaryLabel: () -> String = { context.getString(com.example.cardtally.R.string.record_keypad_confirm) },
    private val onSecondaryAction: (() -> Unit)? = null,
    private val secondaryEnabled: () -> Boolean = { true }
) {
    private var target: EditText = amount
    private val boundTargets = mutableSetOf<EditText>()

    private fun suppressSystemIme() {
        val inputMethod = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val token = target.windowToken ?: keypad.windowToken
        if (token != null) inputMethod.hideSoftInputFromWindow(token, 0)
        listOf(target, keypad, keypad.rootView).forEach { view ->
            ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
        }
    }

    private fun scheduleImeSuppression() {
        suppressSystemIme()
        target.post { suppressSystemIme() }
        target.postDelayed({ suppressSystemIme() }, 200)
        target.postDelayed({ suppressSystemIme() }, 600)
    }

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
        amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !alwaysVisible) show()
            else if (hasFocus) scheduleImeSuppression()
        }
        digitIds.forEach { (digit, id) ->
            keypad.findViewById<View>(id)?.setOnClickListener { insert(digit.toString()) }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_dot)?.setOnClickListener {
            if (!target.text.toString().substringAfterLast('+').substringAfterLast('-').contains('.')) {
                insert(".")
            }
        }
        // Handle these as touch actions instead of relying on a click generated
        // after focus navigation. The keypad is layered over a ScrollView on
        // the recurring form; consuming the gesture on the operator cell keeps
        // its hit area and its action in sync on both touch and accessibility
        // driven interactions.
        val minusKey = keypad.findViewById<View>(com.example.cardtally.R.id.keypad_minus)
        minusKey?.isFocusable = false
        minusKey?.isFocusableInTouchMode = false
        minusKey?.setOnClickListener {
            insertOperator("-")
        }
        minusKey?.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP) insertOperator("-")
            true
        }
        val plusKey = keypad.findViewById<View>(com.example.cardtally.R.id.keypad_plus)
        plusKey?.isFocusable = false
        plusKey?.isFocusableInTouchMode = false
        plusKey?.setOnClickListener {
            insertOperator("+")
        }
        plusKey?.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP) insertOperator("+")
            true
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.keypad_delete)?.setOnClickListener {
            ensureTargetSelection()
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
                when (completionMode) {
                    AmountKeypadCompletionMode.DISMISS_KEYPAD -> {
                        hide()
                        onConfirm()
                    }
                    AmountKeypadCompletionMode.SAVE_RECORD,
                    AmountKeypadCompletionMode.SAVE_ASSET -> onConfirm()
                }
            }
        }
        keypad.findViewById<View>(com.example.cardtally.R.id.btn_save_and_add)?.let { secondary ->
            secondary.visibility = View.VISIBLE
            secondary.isEnabled = onSecondaryAction != null && secondaryEnabled()
            secondary.alpha = if (secondary.isEnabled) 1f else 0.45f
            secondary.importantForAccessibility = if (secondary.isEnabled) {
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            secondary.setOnClickListener { if (secondary.isEnabled) onSecondaryAction?.invoke() }
        }
        updateConfirmLabel()

        if (alwaysVisible) {
            activateTarget(amount)
            keypad.visibility = View.VISIBLE
            scheduleImeSuppression()
        } else {
            keypad.visibility = View.GONE
        }
    }

    fun refreshActions() {
        keypad.findViewById<View>(com.example.cardtally.R.id.btn_save_and_add)?.let { secondary ->
            secondary.visibility = View.VISIBLE
            secondary.isEnabled = onSecondaryAction != null && secondaryEnabled()
            secondary.alpha = if (secondary.isEnabled) 1f else 0.45f
            secondary.importantForAccessibility = if (secondary.isEnabled) {
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        }
        updateConfirmLabel()
    }

    /** Registers another numeric field that shares this keypad. */
    fun bindTarget(editText: EditText) {
        if (!boundTargets.add(editText)) return
        // The shared keypad supports expressions (for example, 3.00+2).
        // A numberDecimal key listener rejects the operator characters before
        // the controller can insert them, so keep the field text-backed while
        // still suppressing the system IME below.
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
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
        if (alwaysVisible) {
            activateTarget(editText)
            scheduleImeSuppression()
        }
    }

    private fun insert(value: String) {
        ensureTargetSelection()
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(0)
        target.text.replace(minOf(start, end), maxOf(start, end), value)
        target.setSelection((minOf(start, end) + value.length).coerceAtMost(target.length()))
    }

    private fun insertOperator(operator: String) {
        ensureTargetSelection()
        val current = target.text.toString()
        if (current.isBlank()) {
            if (allowNegative && operator == "-") insert(operator)
            return
        }
        if (target.selectionStart == 0 && target.selectionEnd == 0) {
            target.setSelection(target.length())
        }
        if (current.last() in charArrayOf('+', '-')) return
        insert(operator)
    }

    private fun ensureTargetSelection() {
        val wasFocused = target.hasFocus()
        if (!wasFocused) {
            target.requestFocus()
            target.setSelection(target.length())
        } else if (target.selectionStart < 0 || target.selectionEnd < 0) {
            target.setSelection(target.length())
        }
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
        val button = keypad.findViewById<TextView>(com.example.cardtally.R.id.keypad_confirm) ?: return
        val hasExpression = target.text.contains('+') || target.text.contains('-')
        button.text = when {
            hasExpression -> context.getString(com.example.cardtally.R.string.record_keypad_equals)
            completionMode == AmountKeypadCompletionMode.DISMISS_KEYPAD -> ""
            else -> primaryLabel()
        }
        button.contentDescription = context.getString(
            when {
                hasExpression -> com.example.cardtally.R.string.record_keypad_calculate
                completionMode == AmountKeypadCompletionMode.DISMISS_KEYPAD -> com.example.cardtally.R.string.record_keypad_hide
                else -> com.example.cardtally.R.string.record_keypad_complete_save
            }
        )
        val icon = keypad.findViewById<android.widget.ImageView>(com.example.cardtally.R.id.keypad_confirm_icon)
        if (icon != null) {
            icon.visibility = if (!hasExpression && completionMode == AmountKeypadCompletionMode.DISMISS_KEYPAD) View.VISIBLE else View.GONE
            icon.setImageResource(com.example.cardtally.R.drawable.tabler_chevron_down)
        }
    }

    fun show() {
        target.showSoftInputOnFocus = false
        if (target.text.toString() == context.getString(com.example.cardtally.R.string.amount_default)) {
            target.setText("")
            target.setSelection(0)
        }
        keypad.visibility = View.VISIBLE
        normalActions?.visibility = View.GONE
        // Amount entry starts from the end of the current value. This also
        // prevents the inputType hand-off from leaving the cursor at index 0,
        // which would turn 3.00 + into +3.00.
        target.setSelection(target.length())
        ensureTargetSelection()
        // Focus changes can schedule an IME show after this callback returns;
        // suppress it both now and after the focus/animation settles.
        scheduleImeSuppression()
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
            suppressSystemIme()
            target.isCursorVisible = false
            target.clearFocus()
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
            scheduleImeSuppression()
        }
    }

    /** Hides the pinned keypad while the soft keyboard (e.g. the note field) is used. */
    fun hideForSoftKeyboard() {
        if (alwaysVisible) {
            target.isCursorVisible = false
            target.clearFocus()
            keypad.visibility = View.GONE
        }
    }

    fun restoreAfterSoftKeyboard() {
        if (alwaysVisible) {
            activateTarget(target)
            keypad.visibility = View.VISIBLE
            scheduleImeSuppression()
        }
    }

    private fun activateTarget(editText: EditText) {
        target = editText
        editText.isCursorVisible = true
        editText.requestFocus()
        editText.setSelection(editText.length())
    }
}
