package com.sms2api.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sms2api.app.data.model.SmsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(record: SmsRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreAll(records: List<SmsRecord>): List<Long>

    @Update
    suspend fun update(record: SmsRecord)

    @Query("UPDATE sms_records SET status = :status, http_code = :httpCode, retry_count = :retryCount WHERE message_id = :messageId")
    suspend fun updateDeliveryResult(
        messageId: String,
        status: String,
        httpCode: Int?,
        retryCount: Int
    ): Int

    @Query("SELECT * FROM sms_records WHERE message_id = :messageId LIMIT 1")
    suspend fun getRecordById(messageId: String): SmsRecord?

    @Query("SELECT * FROM sms_records WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingRecords(limit: Int = 50): List<SmsRecord>

    @Query("SELECT * FROM sms_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<SmsRecord>>

    @Query("SELECT * FROM sms_records WHERE status = :status ORDER BY timestamp DESC")
    fun getRecordsByStatusFlow(status: String): Flow<List<SmsRecord>>

    @Query("SELECT COUNT(*) FROM sms_records WHERE status = :status")
    fun getCountByStatusFlow(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_records")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_records WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sms_records WHERE sender = :sender AND body = :body AND ABS(timestamp - :timestamp) < 10000")
    suspend fun existsSimilarRecord(sender: String, body: String, timestamp: Long): Int

    @Query("SELECT * FROM sms_records WHERE sender = :sender AND body = :body AND ABS(timestamp - :timestamp) < 10000 LIMIT 1")
    suspend fun getSimilarRecord(sender: String, body: String, timestamp: Long): SmsRecord?

    @Query("UPDATE sms_records SET status = :status WHERE message_id = :messageId")
    suspend fun updateStatus(messageId: String, status: String): Int

    @Query("DELETE FROM sms_records WHERE status = 'SUCCESS'")
    suspend fun deleteSuccessful(): Int

    @Query("DELETE FROM sms_records")
    suspend fun deleteAll(): Int

    @Query("UPDATE sms_records SET status = 'PENDING', retry_count = 0 WHERE status = 'FAILED'")
    suspend fun resetFailedToPending(): Int

    @Query("UPDATE sms_records SET status = 'PENDING', retry_count = 0")
    suspend fun resetAllToPending(): Int
}
