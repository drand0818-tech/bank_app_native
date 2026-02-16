package com.example.yonosbi

import org.junit.Test
import org.junit.Assert.*

class SmsParsingTest {

    private fun extractOtp(message: String): String? {
        val regex = "\\b\\d{4,6}\\b".toRegex()
        return regex.find(message)?.value
    }

    private fun extractCardSuffix(message: String): String? {
        val regex = "(?i)(?:ending|card|a/c|acc|account|no\\.?)\\s*(?:in|xx|\\*\\*|\\s)*(\\d{4})\\b".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }

    @Test
    fun testOtpExtraction() {
        assertEquals("123456", extractOtp("Your OTP is 123456"))
        assertEquals("9876", extractOtp("Login code: 9876 and don't share it"))
    }

    @Test
    fun testCardSuffixExtraction() {
        assertEquals("4321", extractCardSuffix("OTP for transaction on card ending 4321 is 123456"))
        assertEquals("9999", extractCardSuffix("Your Card XX9999 has been charged"))
        assertEquals("5555", extractCardSuffix("A/c no. XX5555 OTP is 111222"))
        assertEquals(null, extractCardSuffix("Your verification code is 1234. Do not share."))
    }
}
