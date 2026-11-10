package com.sms2api.app

import com.sms2api.app.data.model.FilterMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRepositoryTest {

    @Test
    fun testKeywordFilterMatching() {
        val keyword = "OTP"
        val body1 = "Your secret OTP is 482910"
        val body2 = "Welcome to our service"

        assertTrue(body1.contains(keyword, ignoreCase = true))
        assertFalse(body2.contains(keyword, ignoreCase = true))
    }

    @Test
    fun testWhitelistMatching() {
        val whitelistStr = "VODAFONE, TURKCELL, +905551234567"
        val whitelist = whitelistStr.split(",", "\n", ";").map { it.trim().lowercase() }

        val sender1 = "VODAFONE"
        val sender2 = "+90 (555) 123 4567"
        val sender3 = "BANK_ALERT"

        val normalized1 = sender1.lowercase().replace("\\s+".toRegex(), "")
        val normalized2 = sender2.lowercase().replace("[^0-9+]".toRegex(), "")
        val normalized3 = sender3.lowercase().replace("\\s+".toRegex(), "")

        assertTrue(whitelist.any { normalized1.contains(it.replace("\\s+".toRegex(), "")) })
        assertTrue(whitelist.any { normalized2.contains(it.replace("[^0-9+]".toRegex(), "")) })
        assertFalse(whitelist.any { normalized3.contains(it.replace("\\s+".toRegex(), "")) })
    }

    @Test
    fun testFilterModes() {
        val modes = FilterMode.values()
        assertTrue(modes.contains(FilterMode.ALL))
        assertTrue(modes.contains(FilterMode.SELECTED_NUMBERS))
    }
}
