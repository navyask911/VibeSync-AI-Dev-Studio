package com.vibesync.promptstudio.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsStoreTest {
    @Test
    fun redactsProviderHeadersBearerTokensAndSecretQueryParameters() {
        val openAi = "Authorization: Bearer sk-12345678901234567890"
        val anthropic = "x-api-key=sk-ant-12345678901234567890ABCDE"
        val gemini = "https://example.invalid/generate?key=AIza123456789012345678901234"
        val redacted = DiagnosticRedactor.redact(openAi + "\n" + anthropic + "\n" + gemini)

        assertFalse(redacted.contains("sk-12345678901234567890"))
        assertFalse(redacted.contains("sk-ant-12345678901234567890ABCDE"))
        assertFalse(redacted.contains("AIza123456789012345678901234"))
        assertTrue(redacted.contains("[REDACTED]") || redacted.contains("[REDACTED_PROVIDER_KEY]"))
    }

    @Test
    fun boundedBufferRetainsOnlyNewestRecords() {
        val buffer = BoundedDiagnosticBuffer(2)
        buffer.add(record(1L, "first"))
        buffer.add(record(2L, "second"))
        buffer.add(record(3L, "third"))

        assertEquals(listOf("second", "third"), buffer.snapshot().map { it.operation })
    }

    @Test
    fun exportedReportRedactsValuesAndExplainsLogcatLimit() {
        val record = record(100L, "request").copy(
            message = "Authorization: Bearer sk-12345678901234567890"
        )
        val report = DiagnosticsStore.buildReport(
            listOf("Android: test", "App-collected records only"),
            listOf(record)
        )

        assertFalse(report.contains("sk-12345678901234567890"))
        assertTrue(report.contains("not device-wide Logcat"))
        assertTrue(report.contains("API keys"))
    }

    private fun record(time: Long, operation: String) = DiagnosticRecord(
        timestampMillis = time,
        severity = DiagnosticSeverity.INFO,
        component = "test",
        category = "unit",
        operation = operation,
        provider = null,
        message = "safe test event",
        processId = 123,
        appVersion = "test"
    )
}
