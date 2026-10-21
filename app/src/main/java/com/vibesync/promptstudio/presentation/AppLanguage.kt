package com.vibesync.promptstudio.presentation

import androidx.annotation.StringRes
import com.vibesync.promptstudio.R

/**
 * Languages intentionally offered by the in-app language selector.
 *
 * Keep this list in sync with res/xml/locales_config.xml and the localized
 * string resources so Android 13+ system settings and the in-app selector agree.
 */
enum class AppLanguage(
    val languageTag: String,
    @StringRes val labelRes: Int,
    val isRightToLeft: Boolean
) {
    ENGLISH("en", R.string.language_english, false),
    SPANISH("es", R.string.language_spanish, false),
    GERMAN("de", R.string.language_german, false),
    HINDI("hi", R.string.language_hindi, false),
    ARABIC("ar", R.string.language_arabic, true);

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage {
            val languageCode = tag?.substringBefore('-')?.lowercase()
            return entries.firstOrNull { it.languageTag == languageCode } ?: ENGLISH
        }
    }
}
