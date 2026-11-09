package com.sms2api.app.data.network

import android.util.Base64
import com.google.gson.Gson
import com.sms2api.app.BuildConfig
import com.sms2api.app.data.local.EncryptedPreferencesManager
import com.sms2api.app.data.model.AuthType
import com.sms2api.app.data.model.SmsPayload
import com.sms2api.app.util.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

data class NetworkResult(
    val isSuccess: Boolean,
    val httpCode: Int?,
    val errorMessage: String? = null
)

class ApiClient(private val preferencesManager: EncryptedPreferencesManager) {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    suspend fun sendPayload(payload: SmsPayload): NetworkResult = withContext(Dispatchers.IO) {
        val rawEndpoint = preferencesManager.apiEndpoint
        if (rawEndpoint.isBlank() || !UrlValidator.isValidUrl(rawEndpoint)) {
            return@withContext NetworkResult(
                isSuccess = false,
                httpCode = null,
                errorMessage = if (rawEndpoint.isBlank()) "API endpoint is not configured" else "Invalid Webhook URL format"
            )
        }

        // Active token retrieval based on configured authentication method
        val activeToken = when (preferencesManager.authType) {
            AuthType.BEARER_TOKEN -> preferencesManager.bearerToken.trim()
            AuthType.CUSTOM_HEADER -> preferencesManager.customHeaders.firstOrNull {
                it.key.equals("X-API-Key", ignoreCase = true) || it.key.equals("token", ignoreCase = true)
            }?.value?.trim().orEmpty()
            else -> ""
        }

        // Automatically inject token query parameter if targeting script.google.com and not already in URL
        var endpoint = rawEndpoint
        if (activeToken.isNotEmpty() && endpoint.contains("script.google.com") &&
            !endpoint.contains("token=") && !endpoint.contains("key=") && !endpoint.contains("auth=")
        ) {
            val separator = if (endpoint.contains("?")) "&" else "?"
            endpoint = "$endpoint${separator}token=$activeToken"
        }

        // Inject token into payload JSON if not already present
        val payloadToSend = if (activeToken.isNotEmpty() && payload.authToken.isNullOrBlank()) {
            payload.copy(authToken = activeToken)
        } else {
            payload
        }

        try {
            val jsonString = gson.toJson(payloadToSend)
            val requestBody = jsonString.toRequestBody(jsonMediaType)
            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "SMS2API-Android-Forwarder/1.0")

            // Apply Authentication Headers safely
            applyAuthHeaders(requestBuilder)

            client.newCall(requestBuilder.build()).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()

                // Check for JSON-level error payload (e.g. Google Apps Script returning 200 with error JSON)
                val isPayloadError = (bodyString.contains("\"status\"") && bodyString.contains("\"error\"")) ||
                        bodyString.contains("\"code\": 401") || bodyString.contains("\"code\":401")

                val isSuccess = response.isSuccessful && !isPayloadError

                val errorMessage = when {
                    isSuccess -> null
                    isPayloadError -> {
                        runCatching {
                            com.google.gson.JsonParser.parseString(bodyString).asJsonObject.get("message")?.asString
                        }.getOrNull() ?: "Server returned error in response payload"
                    }
                    else -> "Server returned HTTP $code"
                }

                NetworkResult(
                    isSuccess = isSuccess,
                    httpCode = code,
                    errorMessage = errorMessage
                )
            }
        } catch (e: Exception) {
            NetworkResult(
                isSuccess = false,
                httpCode = null,
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        }
    }

    suspend fun testConnection(): NetworkResult = withContext(Dispatchers.IO) {
        val testPayload = SmsPayload(
            messageId = "test_${System.currentTimeMillis()}_12345678",
            sender = "TEST_SENDER",
            body = "SMS2API connection verification probe",
            timestamp = System.currentTimeMillis()
        )
        sendPayload(testPayload)
    }

    private fun applyAuthHeaders(builder: Request.Builder) {
        runCatching {
            when (preferencesManager.authType) {
                AuthType.NONE -> {
                    // No extra auth headers
                }
                AuthType.BASIC_AUTH -> {
                    val user = preferencesManager.basicUsername
                    val pass = preferencesManager.basicPassword
                    val credentials = "$user:$pass"
                    val encoded = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    builder.header("Authorization", "Basic $encoded")
                }
                AuthType.BEARER_TOKEN -> {
                    val token = preferencesManager.bearerToken.trim()
                    if (token.isNotEmpty()) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                AuthType.CUSTOM_HEADER -> {
                    preferencesManager.customHeaders.forEach { header ->
                        val key = header.key.trim()
                        val value = header.value.trim()
                        if (key.isNotEmpty()) {
                            builder.header(key, value)
                        }
                    }
                }
            }
        }
    }
}
