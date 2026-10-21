package com.vibesync.promptstudio

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardStartsWithMainWelcomeAndGlobalControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.home_greeting)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_language)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_theme)).assertExists()
    }

    @Test
    fun dashboardLanguagePickerListsAllSupportedLocales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithContentDescription(context.getString(R.string.home_language)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.home_choose_language)).assertExists()
        listOf("English", "Español", "Deutsch", "हिन्दी", "العربية").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }
}
