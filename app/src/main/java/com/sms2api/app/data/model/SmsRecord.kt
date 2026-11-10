package com.sms2api.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_records",
    indices = [
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class SmsRecord(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "status")
    val status: String = DeliveryStatus.PENDING.name,

    @ColumnInfo(name = "http_code")
    val httpCode: Int? = null,

    @ColumnInfo(name = "retry_count", defaultValue = "0")
    val retryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
