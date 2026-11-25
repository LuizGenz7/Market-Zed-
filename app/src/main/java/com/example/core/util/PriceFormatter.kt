package com.example.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

sealed class BudgetParseResult {
    data object Empty : BudgetParseResult()
    data class Valid(val amount: Double) : BudgetParseResult()
    data object NegativeError : BudgetParseResult()
    data object InvalidFormatError : BudgetParseResult()
}

object PriceFormatter {
    private val integerFormatter: NumberFormat
        get() = NumberFormat.getIntegerInstance(Locale.US)

    private val decimalFormatter: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    fun formatListingPrice(
        price: Double?,
        priceMode: String? = null,
        categoryId: String? = null
    ): String {
        val normalizedMode = priceMode?.trim()?.lowercase(Locale.ROOT)
        val isWanted = categoryId?.trim()?.lowercase(Locale.ROOT) == "wanted"
        val isFreeCategory = categoryId?.trim()?.lowercase(Locale.ROOT) == "free"

        if (normalizedMode == "free" || (isFreeCategory && (price == null || price == 0.0))) {
            return "Free"
        }

        if (price == null || price.isNaN() || price < 0.0) {
            return if (isWanted) "Budget not specified" else "Contact for price"
        }

        if (price == 0.0) {
            return if (isWanted) "Budget not specified" else "Free"
        }

        val formattedAmount = formatKwachaAmount(price)
        return if (isWanted) "Budget: $formattedAmount" else formattedAmount
    }

    fun formatKwachaAmount(amount: Double): String {
        val isWhole = abs(amount - amount.toLong()) < 0.005
        val numberStr = if (isWhole) {
            integerFormatter.format(amount.toLong())
        } else {
            decimalFormatter.format(amount)
        }
        return "K$numberStr"
    }

    fun parseStrictBudgetInput(raw: String?): BudgetParseResult {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return BudgetParseResult.Empty

        val stripped = trimmed
            .replace("ZMW", "", ignoreCase = true)
            .replace("zmw", "", ignoreCase = true)
            .replace("K", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", "")

        if (stripped.isEmpty()) return BudgetParseResult.InvalidFormatError
        if (stripped.startsWith("-")) {
            val absNum = stripped.removePrefix("-").toDoubleOrNull()
            return if (absNum != null && absNum > 0.0) {
                BudgetParseResult.NegativeError
            } else if (absNum == 0.0) {
                BudgetParseResult.Valid(0.0)
            } else {
                BudgetParseResult.InvalidFormatError
            }
        }

        // Reject scientific notation or multiple dots/invalid characters
        if (!stripped.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
            val numeric = stripped.toDoubleOrNull()
            return when {
                numeric == null || numeric.isNaN() || numeric.isInfinite() -> BudgetParseResult.InvalidFormatError
                numeric < 0.0 -> BudgetParseResult.NegativeError
                else -> BudgetParseResult.InvalidFormatError
            }
        }

        val parsed = stripped.toDoubleOrNull() ?: return BudgetParseResult.InvalidFormatError
        return when {
            parsed.isNaN() || parsed.isInfinite() -> BudgetParseResult.InvalidFormatError
            parsed < 0.0 -> BudgetParseResult.NegativeError
            else -> BudgetParseResult.Valid(parsed)
        }
    }

    fun parsePriceInput(raw: String?): Double? {
        return when (val res = parseStrictBudgetInput(raw)) {
            is BudgetParseResult.Valid -> res.amount
            else -> null
        }
    }
}
