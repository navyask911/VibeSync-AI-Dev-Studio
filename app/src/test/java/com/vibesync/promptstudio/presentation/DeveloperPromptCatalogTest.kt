package com.vibesync.promptstudio.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperPromptCatalogTest {
    @Test
    fun promptBankContainsAll150PromptsInFiveEqualCategories() {
        assertEquals(150, DeveloperPromptCatalog.prompts.size)
        DeveloperPromptCategory.entries.forEach { category ->
            assertEquals(30, DeveloperPromptCatalog.prompts.count { it.category == category })
        }
        assertEquals(150, DeveloperPromptCatalog.prompts.map { it.id }.distinct().size)
        assertTrue(DeveloperPromptCatalog.prompts.all { it.text.isNotBlank() })
    }

    @Test
    fun commandHubContainsUniqueCopyableReferences() {
        assertTrue(DeveloperCommandCatalog.commands.size >= 15)
        assertEquals(
            DeveloperCommandCatalog.commands.size,
            DeveloperCommandCatalog.commands.map { it.id }.distinct().size
        )
        assertTrue(DeveloperCommandCatalog.commands.all { it.title.isNotBlank() && it.command.isNotBlank() })
    }
}
