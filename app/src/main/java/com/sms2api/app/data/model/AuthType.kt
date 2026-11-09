package com.sms2api.app.data.model

import com.google.gson.annotations.SerializedName

enum class AuthType {
    NONE,
    BASIC_AUTH,
    BEARER_TOKEN,
    CUSTOM_HEADER
}

data class HeaderItem(
    @SerializedName("key")
    val key: String = "",

    @SerializedName("value")
    val value: String = ""
)
