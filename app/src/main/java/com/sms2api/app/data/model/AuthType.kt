package com.sms2api.app.data.model

enum class AuthType {
    NONE,
    BASIC_AUTH,
    BEARER_TOKEN,
    CUSTOM_HEADER
}

data class HeaderItem(
    val key: String = "",
    val value: String = ""
)
