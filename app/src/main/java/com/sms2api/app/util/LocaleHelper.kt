package com.sms2api.app.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.text.layoutDirection
import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val nameInOwnLanguage: String,
    val nameInEnglish: String
) {
    val displayLabel: String
        get() = if (code.isEmpty()) {
            "$nameInOwnLanguage / $nameInEnglish"
        } else {
            "$nameInOwnLanguage ($nameInEnglish)"
        }
}

object LocaleHelper {

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("", "System Default", "System Default"),
        SupportedLanguage("tr", "Türkçe", "Turkish"),
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("zh", "简体中文", "Mandarin Chinese"),
        SupportedLanguage("hi", "हिन्दी", "Hindi"),
        SupportedLanguage("es", "Español", "Spanish"),
        SupportedLanguage("ar", "العربية الفصحى", "Modern Standard Arabic"),
        SupportedLanguage("fr", "Français", "French"),
        SupportedLanguage("bn", "বাংলা", "Bengali"),
        SupportedLanguage("pt", "Português", "Portuguese"),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian"),
        SupportedLanguage("ur", "اردو", "Urdu"),
        SupportedLanguage("ru", "Русский", "Russian"),
        SupportedLanguage("de", "Deutsch", "German"),
        SupportedLanguage("ja", "日本語", "Japanese"),
        SupportedLanguage("ar-EG", "لهجة مصرية", "Egyptian Arabic"),
        SupportedLanguage("vi", "Tiếng Việt", "Vietnamese")
    )

    fun applyLocale(context: Context, languageTag: String): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager != null) {
                if (languageTag.isBlank()) {
                    localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                } else {
                    localeManager.applicationLocales = LocaleList.forLanguageTags(languageTag)
                }
            }
        }

        val targetLocale = getLocale(languageTag)
        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return context.createConfigurationContext(config)
    }

    fun getLocale(languageTag: String): Locale {
        return if (languageTag.isBlank()) {
            Locale.getDefault()
        } else if (languageTag.contains("-")) {
            val parts = languageTag.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale.forLanguageTag(languageTag)
        }
    }
}
