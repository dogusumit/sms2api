package com.sms2api.app.data.model

import com.google.gson.annotations.SerializedName

data class SmsPayload(
    @SerializedName("message_id")
    val messageId: String,

    @SerializedName("sender")
    val sender: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("auth_token")
    val authToken: String? = null
)
