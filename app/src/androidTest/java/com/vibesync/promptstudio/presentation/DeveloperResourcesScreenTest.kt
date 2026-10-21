package com.vibesync.promptstudio.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibesync.promptstudio.R
import com.vibesync.promptstudio.presentation.theme.VibeSyncTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperResourcesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun externalResourceCategoryExpandsAndCollapses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val categoryTitle = context.getString(R.string.developer_resources_android_category)
        composeRule.setContent {
            VibeSyncTheme {
                DeveloperResourcesScreen(
                    onBack = {},
                    onOpenWebStudio = {},
                    onSendPromptToBrowser = {},
                    onOpenToolbox = {},
                    onOpenApiStudio = {},
                    onOpenDiagnostics = {},
                    onOpenSettings = {},
                    initialSection = "services"
                )
            }
        }
        composeRule.onNodeWithText("Android Developers").assertExists()
        composeRule.onNodeWithText(categoryTitle).performClick()
        composeRule.onNodeWithText("Android Developers").assertDoesNotExist()
        composeRule.onNodeWithText(categoryTitle).performClick()
        composeRule.onNodeWithText("Android Developers").assertExists()
    }

    @Test
    fun promptSectionExposesCopyActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            VibeSyncTheme {
                DeveloperResourcesScreen(
                    onBack = {},
                    onOpenWebStudio = {},
                    onSendPromptToBrowser = {},
                    onOpenToolbox = {},
                    onOpenApiStudio = {},
                    onOpenDiagnostics = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.developer_prompt_copy)).assertExists()
    }
}
