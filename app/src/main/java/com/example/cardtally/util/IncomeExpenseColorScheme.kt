package com.example.cardtally.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.cardtally.R

/** One source of truth for the user's income/expense color preference. */
object IncomeExpenseColorScheme {
    const val INCOME_GREEN_EXPENSE_RED = 0
    const val INCOME_RED_EXPENSE_GREEN = 1
    const val BOTH_BLACK = 2

    private const val PREFS = "cardtally_preferences"
    private const val KEY_MODE = "income_expense_color_mode"

    fun getMode(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_MODE, INCOME_RED_EXPENSE_GREEN)
        .coerceIn(INCOME_GREEN_EXPENSE_RED, BOTH_BLACK)

    fun saveMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MODE, mode.coerceIn(INCOME_GREEN_EXPENSE_RED, BOTH_BLACK))
            .apply()
    }

    fun incomePrimary(context: Context): Int = when (getMode(context)) {
        INCOME_GREEN_EXPENSE_RED -> ContextCompat.getColor(context, R.color.income_primary)
        INCOME_RED_EXPENSE_GREEN -> ContextCompat.getColor(context, R.color.expense_primary)
        else -> ContextCompat.getColor(context, R.color.onSurface_light)
    }

    fun expensePrimary(context: Context): Int = when (getMode(context)) {
        INCOME_GREEN_EXPENSE_RED -> ContextCompat.getColor(context, R.color.expense_primary)
        INCOME_RED_EXPENSE_GREEN -> ContextCompat.getColor(context, R.color.income_primary)
        else -> ContextCompat.getColor(context, R.color.onSurface_light)
    }

    fun incomeContainer(context: Context): Int = when (getMode(context)) {
        INCOME_GREEN_EXPENSE_RED -> ContextCompat.getColor(context, R.color.success_container)
        INCOME_RED_EXPENSE_GREEN -> ContextCompat.getColor(context, R.color.error_container)
        else -> ContextCompat.getColor(context, R.color.surface_container_low)
    }

    fun expenseContainer(context: Context): Int = when (getMode(context)) {
        INCOME_GREEN_EXPENSE_RED -> ContextCompat.getColor(context, R.color.error_container)
        INCOME_RED_EXPENSE_GREEN -> ContextCompat.getColor(context, R.color.success_container)
        else -> ContextCompat.getColor(context, R.color.surface_container_low)
    }
}
