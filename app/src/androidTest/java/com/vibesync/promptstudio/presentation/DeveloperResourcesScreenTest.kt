package com.vibesync.promptstudio.presentation

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
    fun searchControlsExpandAndCollapseAccessibly() {
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
                    onOpenSettings = {},
                    showTopBar = false
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.developer_section_prompts)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.developer_search_toggle))
            .performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.developer_search_close))
            .assertExists()
    }

    @Test
    fun tappingPromptAccordionRevealsItsActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prompt = DeveloperPromptCatalog.prompts.first()
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
                    showTopBar = false
                )
            }
        }

        composeRule.onNodeWithTag("resource_prompt_${prompt.id}").performClick()
        composeRule.onNodeWithText(context.getString(R.string.developer_prompt_copy)).assertExists()
    }
}
