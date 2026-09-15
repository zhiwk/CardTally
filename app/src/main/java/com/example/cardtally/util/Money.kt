package com.example.cardtally.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * CNY accounting values cross the UI/database boundary as exact integer fen.
 * UI buffers remain ordinary yuan strings such as "1.22".
 */
object Money {
    private const val SCALE = 2
    private val DECIMAL = Regex("\\d+(?:\\.\\d{1,2})?")

    fun parseYuan(value: String, allowNegative: Boolean = false): Long? {
        val normalized = value.trim()
        val number = if (allowNegative) Regex("-?$DECIMAL") else DECIMAL
        if (!normalized.matches(number)) return null
        return try {
            BigDecimal(normalized)
                .movePointRight(SCALE)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Evaluates the keypad's left-to-right + / - syntax without floating point. */
    fun evaluateYuanExpression(expression: String, allowNegative: Boolean = false): Long? {
        val normalized = expression.replace(" ", "")
        if (normalized.isEmpty()) return null
        val startsNegative = normalized.startsWith('-')
        if (startsNegative && !allowNegative) return null
        val unsignedExpression = normalized.removePrefix("-")
        if (!unsignedExpression.matches(Regex("\\d+(?:\\.\\d{1,2})?([+-]\\d+(?:\\.\\d{1,2})?)*"))) {
            return null
        }
        val tokens = unsignedExpression.split(Regex("(?=[+-])|(?<=[+-])"))
        var result = parseYuan(tokens.firstOrNull().orEmpty()) ?: return null
        if (startsNegative) result = -result
        var index = 1
        while (index + 1 < tokens.size) {
            val operand = parseYuan(tokens[index + 1]) ?: return null
            result = try {
                when (tokens[index]) {
                    "+" -> Math.addExact(result, operand)
                    "-" -> Math.subtractExact(result, operand)
                    else -> return null
                }
            } catch (_: ArithmeticException) {
                return null
            }
            index += 2
        }
        return result
    }

    fun formatYuan(minor: Long): String {
        val negative = minor < 0
        val absolute = if (negative) -minor else minor
        val yuan = absolute / 100
        val fen = absolute % 100
        return (if (negative) "-" else "") + yuan + "." + fen.toString().padStart(2, '0')
    }

    fun formatYuan(yuan: Double): String = toMinor(yuan)?.let(::formatYuan) ?: "0.00"

    fun toMinor(yuan: Double): Long? {
        if (!yuan.isFinite()) return null
        return try {
            BigDecimal.valueOf(yuan)
                .movePointRight(SCALE)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun toMajorDouble(minor: Long): Double = minor / 100.0
}
