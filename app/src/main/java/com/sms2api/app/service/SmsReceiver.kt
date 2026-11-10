package com.sms2api.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sms2api.app.SMS2ApiApplication
import com.sms2api.app.data.model.DeliveryStatus
import com.sms2api.app.util.UrlValidator
import com.sms2api.app.worker.SmsForwardWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? SMS2ApiApplication ?: return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multipart SMS parts by originating address and timestamp
        val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "UNKNOWN" }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val endpoint = app.preferencesManager.apiEndpoint.trim()
                val isUrlValid = endpoint.isNotEmpty() && UrlValidator.isValidUrl(endpoint)

                for ((sender, parts) in messagesBySender) {
                    val fullBody = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    if (fullBody.isBlank() || sender.isBlank()) continue

                    // Check filter criteria
                    val matches = app.repository.matchesFilter(sender, fullBody, context)
                    val status = when {
                        !matches -> DeliveryStatus.SKIPPED.name
                        !isUrlValid -> DeliveryStatus.FAILED.name
                        else -> DeliveryStatus.PENDING.name
                    }
                    val record = app.repository.saveIncomingSmsWithStatus(sender, fullBody, timestamp, status)
                    if (matches && isUrlValid) {
                        // Trigger immediate dispatch via WorkManager
                        SmsForwardWorker.enqueue(context, record?.messageId)
                    }
                }
            } catch (e: Exception) {
                // Log or handle any receiver exception
            } finally {
                pendingResult.finish()
            }
        }
    }
}
