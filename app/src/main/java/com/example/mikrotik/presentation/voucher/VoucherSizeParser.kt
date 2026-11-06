package com.example.mikrotik.presentation.voucher

import java.math.BigDecimal
import java.math.RoundingMode

enum class VoucherDataUnit(val label: String, val bytesMultiplier: Long) {
    MB("MB", 1024L * 1024L),
    GB("GB", 1024L * 1024L * 1024L)
}

object VoucherSizeParser {
    private val validNumber = Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")

    /** Converts a positive decimal size to bytes; accepts Arabic and Persian digits too. */
    fun toBytes(input: String, unit: VoucherDataUnit): Long? {
        val normalized = buildString {
            input.trim().forEach { char ->
                when {
                    char.isWhitespace() -> Unit
                    char == ',' || char == '٫' -> append('.')
                    char == '٬' -> Unit
                    Character.digit(char, 10) >= 0 -> append(Character.digit(char, 10))
                    else -> append(char)
                }
            }
        }
        if (!validNumber.matches(normalized)) return null

        return try {
            val value = BigDecimal(normalized)
            if (value <= BigDecimal.ZERO) return null
            value.multiply(BigDecimal.valueOf(unit.bytesMultiplier))
                .setScale(0, RoundingMode.CEILING)
                .longValueExact()
                .takeIf { it > 0L }
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }
}
