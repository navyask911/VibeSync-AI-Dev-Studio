package com.vibesync.promptstudio

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibesync.promptstudio.presentation.AppLanguage
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationSwitchesToPromptBankAndMarksSelectedTab() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithTag("nav_developerhub").performClick()
        composeRule.onNodeWithTag("nav_developerhub").assertIsSelected()
        composeRule.onNodeWithText(context.getString(R.string.developer_section_prompts)).assertExists()
    }

    @Test
    fun globalLanguageSelectorListsAllSupportedLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithTag("global_language_selector").performClick()
        composeRule.onNodeWithText(context.getString(R.string.language_selector_title)).assertExists()
        AppLanguage.entries.forEach { language ->
            composeRule.onNodeWithTag("language_${language.languageTag}").assertExists()
            composeRule.onNodeWithText(context.getString(language.labelRes)).assertExists()
        }
    }

    @Test
    fun everySupportedLocaleHasNavigationAndLanguageStrings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requiredStrings = listOf(
            R.string.language_selector_title,
            R.string.language_selector_open,
            R.string.nav_web_studio,
            R.string.nav_prompt_bank,
            R.string.nav_api_studio,
            R.string.nav_developer_tools,
            R.string.nav_settings,
            R.string.developer_search_toggle,
            R.string.developer_accordion_expand,
            R.string.developer_accordion_collapse
        )

        AppLanguage.entries.forEach { language ->
            val configuration = Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(language.languageTag))
            }
            val localizedContext = context.createConfigurationContext(configuration)
            requiredStrings.forEach { resourceId ->
                assertFalse(
                    "Blank string resource $resourceId for ${language.languageTag}",
                    localizedContext.getString(resourceId).isBlank()
                )
            }
        }
    }
}
