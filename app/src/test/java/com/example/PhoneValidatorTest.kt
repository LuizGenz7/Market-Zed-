package com.example

import com.example.core.util.PhoneValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhoneValidatorTest {

    @Test
    fun `normalizes valid Zambian phone formats`() {
        assertEquals("260971234567", PhoneValidator.normalizeZambianNumber("+260971234567"))
        assertEquals("260971234567", PhoneValidator.normalizeZambianNumber("260971234567"))
        assertEquals("260971234567", PhoneValidator.normalizeZambianNumber("0971234567"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("0969331234"))
        assertEquals("260969333123", PhoneValidator.normalizeZambianNumber("969333123"))
        assertEquals("260961234567", PhoneValidator.normalizeZambianNumber("+260 96 123-4567"))
        assertEquals("260969333123", PhoneValidator.normalizeZambianNumber("00260 969-333-123"))
        assertNull(PhoneValidator.normalizeZambianNumber("12345"))
    }

    @Test
    fun `validates Zambian contact numbers and email`() {
        assertTrue(PhoneValidator.isValidZambianPhone("+260971234567"))
        assertTrue(PhoneValidator.isValidZambianPhone("260971234567"))
        assertTrue(PhoneValidator.isValidZambianPhone("0971234567"))
        assertTrue(PhoneValidator.isValidZambianPhone("969333123"))
        assertFalse(PhoneValidator.isValidZambianPhone("+14155550199"))

        assertTrue(PhoneValidator.isValidEmail("buyer@marketzed.zm"))
        assertFalse(PhoneValidator.isValidEmail("invalid-email"))
    }

    @Test
    fun `builds tel whatsapp and mailto URIs`() {
        val tel = PhoneValidator.buildTelUri("0971234567")
        assertNotNull(tel)
        assertEquals("tel:+260971234567", tel.toString())

        val wa = PhoneValidator.buildWhatsAppUri("969333123", "iPhone 13")
        assertNotNull(wa)
        assertTrue(wa.toString().startsWith("https://wa.me/260969333123"))

        val mail = PhoneValidator.buildMailtoUri("seller@example.com", "Solar Inverter")
        assertNotNull(mail)
        assertTrue(mail.toString().startsWith("mailto:seller@example.com"))
    }
}
