package com.vibesync.promptstudio.presentation.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidDeveloperResourceCatalogTest {
    @Test
    fun catalogContainsTwelveUniqueHttpsResources() {
        val resources = AndroidDeveloperResourceCatalog.resources
        assertEquals(12, resources.size)
        assertEquals(resources.size, resources.map { it.id }.toSet().size)
        assertEquals(resources.size, resources.map { it.url }.toSet().size)
        assertTrue(resources.all { it.url.startsWith("https://") })
    }

    @Test
    fun catalogCoversAllFourCategoriesWithExpectedCounts() {
        val resources = AndroidDeveloperResourceCatalog.resources
        assertEquals(AndroidResourceCategory.entries.toSet(), resources.map { it.category }.toSet())
        assertEquals(2, resources.count { it.category == AndroidResourceCategory.OFFICIAL_CORE })
        assertEquals(4, resources.count { it.category == AndroidResourceCategory.NEWSLETTERS_BLOGS })
        assertEquals(4, resources.count { it.category == AndroidResourceCategory.UTILITIES_PRODUCTIVITY })
        assertEquals(2, resources.count { it.category == AndroidResourceCategory.COMMUNITY_DISCUSSION })
    }
}
