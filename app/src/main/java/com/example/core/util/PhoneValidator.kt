package com.example.core.util

import android.net.Uri
import android.util.Patterns

object PhoneValidator {
    private val zambianRegex = Regex("^260[1-9]\\d{5,9}$")

    fun normalizeZambianNumber(raw: String): String? {
        val stripped = raw.trim().replace(Regex("[\\s\\-()]"), "")
        if (stripped.isEmpty()) return null
        val normalized = when {
            stripped.startsWith("+2600") && stripped.length >= 13 -> "260" + stripped.drop(5)
            stripped.startsWith("+260") -> stripped.removePrefix("+")
            stripped.startsWith("002600") && stripped.length >= 14 -> "260" + stripped.drop(6)
            stripped.startsWith("00260") -> "260" + stripped.removePrefix("00260")
            stripped.startsWith("2600") && stripped.length >= 12 -> "260" + stripped.drop(4)
            stripped.startsWith("260") -> stripped
            stripped.startsWith("0") -> "260" + stripped.drop(1)
            stripped.all { it.isDigit() } -> "260$stripped"
            else -> stripped
        }
        return if (zambianRegex.matches(normalized)) normalized else null
    }

    fun autoFixZambianInput(raw: String): String = normalizeZambianNumber(raw) ?: raw

    fun isValidZambianPhone(raw: String): Boolean = normalizeZambianNumber(raw) != null

    fun isValidEmail(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        return Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun buildTelUri(phone: String): Uri? {
        val normalized = normalizeZambianNumber(phone)
        if (normalized != null) return Uri.parse("tel:+$normalized")
        val cleaned = phone.trim().replace(Regex("[\\s\\-()]"), "")
        if (cleaned.isEmpty()) return null
        val withPlus = if (cleaned.startsWith("+")) cleaned else "+$cleaned"
        return Uri.parse("tel:$withPlus")
    }

    fun buildWhatsAppUri(whatsapp: String, listingTitle: String? = null): Uri? {
        val digits = normalizeZambianNumber(whatsapp)
            ?: whatsapp.trim().replace("+", "").replace(Regex("[\\s\\-()]"), "")
        if (digits.isEmpty()) return null
        val textParam = if (!listingTitle.isNullOrBlank()) {
            "?text=${Uri.encode("Hello, I saw your MarketZed listing: $listingTitle")}"
        } else {
            ""
        }
        return Uri.parse("https://wa.me/$digits$textParam")
    }

    fun buildMailtoUri(email: String, listingTitle: String? = null): Uri? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return null
        val subject = if (!listingTitle.isNullOrBlank()) {
            "?subject=${Uri.encode("MarketZed inquiry: $listingTitle")}"
        } else {
            ""
        }
        return Uri.parse("mailto:$trimmed$subject")
    }
}
