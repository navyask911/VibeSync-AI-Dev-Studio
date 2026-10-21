package com.vibesync.promptstudio

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleResourceCompletenessTest {
    private val requiredKeys = listOf(
        "language_picker_title", "language_picker_content_description",
        "language_english", "language_spanish", "language_german", "language_hindi", "language_arabic",
        "nav_web_studio", "nav_api_diagnostics", "nav_prompt_bank", "nav_resources",
        "developer_resources_search", "developer_search_show", "developer_search_hide",
        "browser_auth_external_notice", "oauth_callback_success_message", "oauth_callback_failure_message",
        "oauth_error_client_id_required", "oauth_error_authorize_https", "oauth_error_token_https",
        "oauth_error_redirect_uri", "oauth_error_scope_required", "language_rtl_note"
    )

    @Test
    fun everySupportedLocaleIncludesAllNewUserVisibleStrings() {
        val roots = listOf(File("app/src/main/res"), File("src/main/res"), File("../app/src/main/res"))
        val resourceRoot = roots.firstOrNull { File(it, "values/strings.xml").isFile }
            ?: throw AssertionError("Could not locate Android resources from test working directory")
        listOf("values", "values-es", "values-de", "values-hi", "values-ar").forEach { locale ->
            val file = File(resourceRoot, "$locale/strings.xml")
            assertTrue("Missing locale resource file: $locale", file.isFile)
            val xml = file.readText()
            requiredKeys.forEach { key -> assertTrue("Missing string '$key' in $locale", xml.contains("name=\"$key\"")) }
        }
    }
}
