package com.vibesync.promptstudio

import com.vibesync.promptstudio.R
import java.util.Locale

internal data class AppLanguageOption(val tag: String, val labelRes: Int)

internal val supportedAppLanguages = listOf(
    AppLanguageOption("en", R.string.language_english),
    AppLanguageOption("es", R.string.language_spanish),
    AppLanguageOption("de", R.string.language_german),
    AppLanguageOption("hi", R.string.language_hindi),
    AppLanguageOption("ar", R.string.language_arabic)
)

internal fun normalizeAppLanguageTag(languageTag: String): String {
    val language = languageTag.substringBefore(',').substringBefore('-').lowercase(Locale.ROOT)
    return supportedAppLanguages.firstOrNull { it.tag == language }?.tag ?: "en"
}

internal fun isAppLanguageRtl(languageTag: String): Boolean =
    normalizeAppLanguageTag(languageTag) == "ar"
