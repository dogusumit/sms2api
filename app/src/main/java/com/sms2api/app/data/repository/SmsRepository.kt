package com.sms2api.app.data.repository

import android.content.Context
import android.provider.Telephony
import com.sms2api.app.data.local.EncryptedPreferencesManager
import com.sms2api.app.data.local.SmsDao
import com.sms2api.app.data.model.DeliveryStatus
import com.sms2api.app.data.model.FilterMode
import com.sms2api.app.data.model.SmsRecord
import com.sms2api.app.data.network.ApiClient
import com.sms2api.app.data.network.PayloadGenerator
import com.sms2api.app.util.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SmsRepository(
    private val smsDao: SmsDao,
    private val apiClient: ApiClient,
    private val preferencesManager: EncryptedPreferencesManager
) {

    val allRecordsFlow: Flow<List<SmsRecord>> = smsDao.getAllRecordsFlow()
    val totalCountFlow: Flow<Int> = smsDao.getTotalCountFlow()
    val pendingCountFlow: Flow<Int> = smsDao.getCountByStatusFlow(DeliveryStatus.PENDING.name)
    val successCountFlow: Flow<Int> = smsDao.getCountByStatusFlow(DeliveryStatus.SUCCESS.name)
    val failedCountFlow: Flow<Int> = smsDao.getCountByStatusFlow(DeliveryStatus.FAILED.name)

    fun getRecordsByStatusFlow(status: DeliveryStatus): Flow<List<SmsRecord>> {
        return smsDao.getRecordsByStatusFlow(status.name)
    }

    /**
     * Evaluates incoming SMS against user filter settings:
     * - Filter mode: ALL, SELECTED_NUMBERS, CONTACT_LIST
     * - Optional keyword filter
     */
    fun matchesFilter(sender: String, body: String, context: Context): Boolean {
        // Keyword filter check
        val keyword = preferencesManager.keywordFilter.trim()
        if (keyword.isNotEmpty() && !body.contains(keyword, ignoreCase = true)) {
            return false
        }

        return when (preferencesManager.filterMode) {
            FilterMode.ALL -> true
            FilterMode.SELECTED_NUMBERS -> {
                val whitelist = preferencesManager.whitelistNumbers
                    .split(",", "\n", ";")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                if (whitelist.isEmpty()) return true

                val normalizedSender = sender.trim().lowercase().replace("\\s+".toRegex(), "")
                whitelist.any { item ->
                    val cleanItem = item.replace("\\s+".toRegex(), "")
                    normalizedSender.contains(cleanItem) || cleanItem.contains(normalizedSender)
                }
            }
        }
    }

    /**
     * Scenario A: Real-Time Incoming SMS Capture
     * Computes deterministic message_id and inserts into Room DB as PENDING.
     */
    suspend fun saveIncomingSms(sender: String, body: String, timestamp: Long): SmsRecord? = withContext(Dispatchers.IO) {
        val messageId = PayloadGenerator.generateMessageId(sender, body, timestamp)
        val record = SmsRecord(
            messageId = messageId,
            sender = sender,
            body = body,
            timestamp = timestamp,
            status = DeliveryStatus.PENDING.name,
            httpCode = null,
            retryCount = 0,
            createdAt = System.currentTimeMillis()
        )
        val rowId = smsDao.insertOrIgnore(record)
        if (rowId != -1L) {
            record
        } else {
            smsDao.getRecordById(messageId)
        }
    }

    suspend fun saveIncomingSmsWithStatus(sender: String, body: String, timestamp: Long, status: String): SmsRecord? = withContext(Dispatchers.IO) {
        val messageId = PayloadGenerator.generateMessageId(sender, body, timestamp)
        val record = SmsRecord(
            messageId = messageId,
            sender = sender,
            body = body,
            timestamp = timestamp,
            status = status,
            httpCode = null,
            retryCount = 0,
            createdAt = System.currentTimeMillis()
        )
        val rowId = smsDao.insertOrIgnore(record)
        if (rowId != -1L) {
            record
        } else {
            smsDao.getRecordById(messageId)
        }
    }

    /**
     * Dispatches pending records sequentially:
     * - HTTP 200..299: mark SUCCESS
     * - HTTP Error / Exception: remain PENDING (or FAILED if retries exceed threshold), increment retry_count
     */
    suspend fun dispatchPendingRecords(maxBatchSize: Int = 50, maxRetries: Int = 10): DeliverySummary = withContext(Dispatchers.IO) {
        val pendingList = smsDao.getPendingRecords(limit = maxBatchSize)
        var successCount = 0
        var failureCount = 0
        val endpoint = preferencesManager.apiEndpoint.trim()
        val isUrlValid = endpoint.isNotEmpty() && UrlValidator.isValidUrl(endpoint)

        for (record in pendingList) {
            if (!isUrlValid) {
                smsDao.updateDeliveryResult(
                    messageId = record.messageId,
                    status = DeliveryStatus.FAILED.name,
                    httpCode = null,
                    retryCount = record.retryCount
                )
                failureCount++
                continue
            }

            val payload = PayloadGenerator.toPayload(record)
            val result = apiClient.sendPayload(payload)

            if (result.isSuccess) {
                smsDao.updateDeliveryResult(
                    messageId = record.messageId,
                    status = DeliveryStatus.SUCCESS.name,
                    httpCode = result.httpCode,
                    retryCount = record.retryCount + 1
                )
                successCount++
            } else {
                val isConfigError = result.errorMessage == "API endpoint is not configured" ||
                        result.errorMessage == "Invalid Webhook URL format"
                val newRetryCount = if (isConfigError) record.retryCount else record.retryCount + 1
                val newStatus = if (isConfigError || newRetryCount >= maxRetries) {
                    DeliveryStatus.FAILED.name
                } else {
                    DeliveryStatus.PENDING.name
                }

                smsDao.updateDeliveryResult(
                    messageId = record.messageId,
                    status = newStatus,
                    httpCode = result.httpCode,
                    retryCount = newRetryCount
                )
                failureCount++
            }
        }

        DeliverySummary(
            totalProcessed = pendingList.size,
            successful = successCount,
            failed = failureCount
        )
    }

    /**
     * Dispatches a single record by messageId immediately
     */
    suspend fun dispatchSingleRecord(messageId: String): Boolean = withContext(Dispatchers.IO) {
        val record = smsDao.getRecordById(messageId) ?: return@withContext false
        val endpoint = preferencesManager.apiEndpoint.trim()
        val isUrlValid = endpoint.isNotEmpty() && UrlValidator.isValidUrl(endpoint)

        if (!isUrlValid) {
            smsDao.updateDeliveryResult(
                messageId = record.messageId,
                status = DeliveryStatus.FAILED.name,
                httpCode = null,
                retryCount = record.retryCount
            )
            return@withContext false
        }

        val payload = PayloadGenerator.toPayload(record)
        val result = apiClient.sendPayload(payload)

        val newRetryCount = record.retryCount + 1
        if (result.isSuccess) {
            smsDao.updateDeliveryResult(
                messageId = record.messageId,
                status = DeliveryStatus.SUCCESS.name,
                httpCode = result.httpCode,
                retryCount = newRetryCount
            )
            true
        } else {
            val isConfigError = result.errorMessage == "API endpoint is not configured" ||
                    result.errorMessage == "Invalid Webhook URL format"
            val newStatus = if (isConfigError || newRetryCount >= 10) {
                DeliveryStatus.FAILED.name
            } else {
                DeliveryStatus.PENDING.name
            }
            smsDao.updateDeliveryResult(
                messageId = record.messageId,
                status = newStatus,
                httpCode = result.httpCode,
                retryCount = if (isConfigError) record.retryCount else newRetryCount
            )
            false
        }
    }

    /**
     * Scenario B: Historical Bulk Transfer ("All Past SMS")
     * Reads inbox, filters, generates deterministic IDs, and executes INSERT OR IGNORE.
     */
    suspend fun importHistoricalInbox(context: Context): BulkImportResult = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var matchingFilter = 0
        var newlyQueued = 0
        val endpoint = preferencesManager.apiEndpoint.trim()
        val isUrlValid = endpoint.isNotEmpty() && UrlValidator.isValidUrl(endpoint)

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            val recordsToInsert = mutableListOf<SmsRecord>()

            cursor?.use {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    totalScanned++
                    val sender = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val timestamp = it.getLong(dateIdx)

                    if (sender.isBlank() || body.isBlank()) continue

                    val matches = matchesFilter(sender, body, context)
                    if (matches) matchingFilter++

                    val targetStatus = when {
                        !matches -> DeliveryStatus.SKIPPED.name
                        !isUrlValid -> DeliveryStatus.FAILED.name
                        else -> DeliveryStatus.PENDING.name
                    }

                    val existingRecord = smsDao.getSimilarRecord(sender, body, timestamp)
                    if (existingRecord != null) {
                        // If it now matches the new filter but was previously SKIPPED, update its status so it can be sent!
                        if (matches && existingRecord.status == DeliveryStatus.SKIPPED.name) {
                            smsDao.updateStatus(existingRecord.messageId, targetStatus)
                            if (targetStatus == DeliveryStatus.PENDING.name) {
                                newlyQueued++
                            }
                        }
                        continue
                    }

                    val messageId = PayloadGenerator.generateMessageId(sender, body, timestamp)
                    recordsToInsert.add(
                        SmsRecord(
                            messageId = messageId,
                            sender = sender,
                            body = body,
                            timestamp = timestamp,
                            status = targetStatus,
                            httpCode = null,
                            retryCount = 0,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            if (recordsToInsert.isNotEmpty()) {
                val rowIds = smsDao.insertOrIgnoreAll(recordsToInsert)
                val newlyInsertedPending = recordsToInsert.filterIndexed { index, rec ->
                    rowIds.getOrNull(index) != -1L && rec.status == DeliveryStatus.PENDING.name
                }.size
                newlyQueued += newlyInsertedPending
            }

            BulkImportResult(
                totalScanned = totalScanned,
                matched = matchingFilter,
                newlyQueued = newlyQueued,
                success = true
            )
        } catch (e: Exception) {
            BulkImportResult(
                totalScanned = totalScanned,
                matched = matchingFilter,
                newlyQueued = newlyQueued,
                success = false,
                errorMessage = e.message
            )
        }
    }

    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        smsDao.getPendingCount()
    }

    suspend fun clearSuccessful() = withContext(Dispatchers.IO) {
        smsDao.deleteSuccessful()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        smsDao.deleteAll()
    }

    suspend fun retryAllFailed() = withContext(Dispatchers.IO) {
        smsDao.resetFailedToPending()
    }

    suspend fun resetAllToPending() = withContext(Dispatchers.IO) {
        smsDao.resetAllToPending()
    }
}

data class DeliverySummary(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int
)

data class BulkImportResult(
    val totalScanned: Int,
    val matched: Int,
    val newlyQueued: Int,
    val success: Boolean,
    val errorMessage: String? = null
)
