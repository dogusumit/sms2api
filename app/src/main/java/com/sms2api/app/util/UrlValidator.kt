package com.sms2api.app.util

import java.net.URI

object UrlValidator {
    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return runCatching {
            val uri = URI(url.trim())
            val parsedUrl = uri.toURL()
            (parsedUrl.protocol == "http" || parsedUrl.protocol == "https")
        }.getOrDefault(false)
    }
}
