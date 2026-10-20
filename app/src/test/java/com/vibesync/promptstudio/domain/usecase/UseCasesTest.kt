package com.vibesync.promptstudio.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {
    private val substitute = SubstituteVariablesUseCase()
    private val counter = TokenCounterUseCase()

    @Test
    fun substitutesKnownVariablesAndReportsUnknownOnes() {
        val result = substitute("Hello {{name}} from {{city}}", mapOf("name" to "Ada"))
        assertEquals("Hello Ada from {{city}}", result.text)
        assertEquals(setOf("city"), result.unresolvedVariables)
    }

    @Test
    fun onlyCompleteVariablesWithValidNamesAreSubstituted() {
        val template = "{{name}} {{first_name}} {{item2}} {{2invalid}} {{hyphen-name}} {{unfinished"
        val result = substitute(
            template,
            mapOf("name" to "Ada", "first_name" to "Grace", "item2" to "book")
        )

        assertEquals(
            "Ada Grace book {{2invalid}} {{hyphen-name}} {{unfinished",
            result.text
        )
        assertTrue(result.unresolvedVariables.isEmpty())
    }

    @Test
    fun variableParserKeepsEqualsInValuesAndIgnoresInvalidNames() {
        assertEquals(
            mapOf("token" to "abc=123", "user_name" to "Ada"),
            substitute.parseVariableLines("token=abc=123\nnot-valid!=skip\nuser_name=Ada")
        )
    }

    @Test
    fun tokenCounterReturnsZeroForBlankAndPositiveForText() {
        assertEquals(0, counter.count(" \n "))
        assertTrue(counter.count("Compose is declarative.") > 0)
    }
}
