package com.sms2api.app

import com.sms2api.app.data.network.PayloadGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PayloadGeneratorTest {

    @Test
    fun testDeterministicMessageIdGeneration() {
        val sender = "VODAFONE"
        val body = "tuketim borcunuz 767.50 tl olup 10 gün içerisinde ödemeniz gerekmektedir"
        val timestamp = 1711234567890L

        val id1 = PayloadGenerator.generateMessageId(sender, body, timestamp)
        val id2 = PayloadGenerator.generateMessageId(sender, body, timestamp)

        // Must be deterministic across calls
        assertEquals(id1, id2)

        // Must begin with "sms_${timestamp}_"
        assertTrue(id1.startsWith("sms_${timestamp}_"))

        // Verify SHA-256 first 8 characters manually
        val raw = "$sender|$body|$timestamp"
        val md = MessageDigest.getInstance("SHA-256")
        val expectedHash = md.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val expectedFirst8 = expectedHash.take(8)

        assertEquals("sms_${timestamp}_${expectedFirst8}", id1)
    }

    @Test
    fun testDifferentAttributesProduceDifferentIds() {
        val idA = PayloadGenerator.generateMessageId("VODAFONE", "Message 1", 1000L)
        val idB = PayloadGenerator.generateMessageId("TURKCELL", "Message 1", 1000L)
        val idC = PayloadGenerator.generateMessageId("VODAFONE", "Message 2", 1000L)
        val idD = PayloadGenerator.generateMessageId("VODAFONE", "Message 1", 2000L)

        assertTrue(idA != idB)
        assertTrue(idA != idC)
        assertTrue(idA != idD)
    }
}
