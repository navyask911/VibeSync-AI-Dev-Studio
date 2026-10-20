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
