package com.sms2api.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sms2api.app.data.model.AuthType
import com.sms2api.app.data.model.FilterMode
import com.sms2api.app.data.model.HeaderItem

class EncryptedPreferencesManager(context: Context) {

    private val gson = Gson()

    private val prefs: SharedPreferences = createEncryptedSharedPreferencesSafely(context)

    companion object {
        private const val TAG = "EncryptedPrefs"
        private const val PREFS_FILE_NAME = "sms2api_secure_prefs"

        private const val KEY_API_ENDPOINT = "api_endpoint"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_BASIC_USERNAME = "basic_username"
        private const val KEY_BASIC_PASSWORD = "basic_password"
        private const val KEY_BEARER_TOKEN = "bearer_token"
        private const val KEY_CUSTOM_HEADERS = "custom_headers_json"
        private const val KEY_FILTER_MODE = "filter_mode"
        private const val KEY_WHITELIST = "whitelist_numbers"
        private const val KEY_KEYWORD_FILTER = "keyword_filter"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_BOOT_AUTOSTART = "boot_autostart"
        private const val KEY_PERIODIC_SYNC = "periodic_sync"
        private const val KEY_APP_LANGUAGE = "app_language"

        private fun createEncryptedSharedPreferencesSafely(context: Context): SharedPreferences {
            return try {
                buildEncryptedSharedPreferences(context)
            } catch (e: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences init failed, re-creating MasterKey", e)
                try {
                    context.deleteSharedPreferences(PREFS_FILE_NAME)
                    buildEncryptedSharedPreferences(context)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback to standard SharedPreferences", e2)
                    context.getSharedPreferences("${PREFS_FILE_NAME}_fallback", Context.MODE_PRIVATE)
                }
            }
        }

        private fun buildEncryptedSharedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    var apiEndpoint: String
        get() = prefs.getString(KEY_API_ENDPOINT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_ENDPOINT, value.trim()) }

    var authType: AuthType
        get() {
            val name = prefs.getString(KEY_AUTH_TYPE, AuthType.NONE.name) ?: AuthType.NONE.name
            return runCatching { AuthType.valueOf(name) }.getOrDefault(AuthType.NONE)
        }
        set(value) = prefs.edit { putString(KEY_AUTH_TYPE, value.name) }

    var basicUsername: String
        get() = prefs.getString(KEY_BASIC_USERNAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BASIC_USERNAME, value) }

    var basicPassword: String
        get() = prefs.getString(KEY_BASIC_PASSWORD, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BASIC_PASSWORD, value) }

    var bearerToken: String
        get() = prefs.getString(KEY_BEARER_TOKEN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BEARER_TOKEN, value) }

    var customHeaders: List<HeaderItem>
        get() {
            val json = prefs.getString(KEY_CUSTOM_HEADERS, null) ?: return emptyList()
            return runCatching {
                val type = object : TypeToken<List<HeaderItem>>() {}.type
                gson.fromJson<List<HeaderItem>>(json, type) ?: emptyList()
            }.getOrDefault(emptyList())
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit { putString(KEY_CUSTOM_HEADERS, json) }
        }

    var filterMode: FilterMode
        get() {
            val name = prefs.getString(KEY_FILTER_MODE, FilterMode.ALL.name) ?: FilterMode.ALL.name
            return runCatching { FilterMode.valueOf(name) }.getOrDefault(FilterMode.ALL)
        }
        set(value) = prefs.edit { putString(KEY_FILTER_MODE, value.name) }

    var whitelistNumbers: String
        get() = prefs.getString(KEY_WHITELIST, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WHITELIST, value) }

    var keywordFilter: String
        get() = prefs.getString(KEY_KEYWORD_FILTER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_KEYWORD_FILTER, value) }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_ENABLED, value) }

    var isBootAutostartEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOOT_AUTOSTART, true)
        set(value) = prefs.edit { putBoolean(KEY_BOOT_AUTOSTART, value) }

    var isPeriodicSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_SYNC, true)
        set(value) = prefs.edit { putBoolean(KEY_PERIODIC_SYNC, value) }

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_APP_LANGUAGE, value.trim()) }
}
