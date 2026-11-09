package com.sms2api.app.data.network

import com.sms2api.app.data.model.SmsPayload
import com.sms2api.app.data.model.SmsRecord
import java.security.MessageDigest

object PayloadGenerator {

    /**
     * Generates a deterministic message_id using SHA-256 with exact timestamp:
     * Raw string format: "${sender}|${body}|${timestamp}"
     * Result format: "sms_${timestamp}_${sha256_first_8_chars}"
     */
    fun generateMessageId(sender: String, body: String, timestamp: Long): String {
        val raw = "$sender|$body|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        val first8Chars = hexString.take(8)
        return "sms_${timestamp}_${first8Chars}"
    }

    fun toPayload(record: SmsRecord, authToken: String? = null): SmsPayload {
        return SmsPayload(
            messageId = record.messageId,
            sender = record.sender,
            body = record.body,
            timestamp = record.timestamp,
            authToken = authToken
        )
    }
}
