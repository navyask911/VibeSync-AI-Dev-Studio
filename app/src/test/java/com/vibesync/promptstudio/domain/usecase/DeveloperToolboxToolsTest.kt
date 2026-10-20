package com.vibesync.promptstudio.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class DeveloperToolboxToolsTest {
    @Test
    fun jsonFormatterPrettyAndMinified() {
        assertEquals(
            "{\n  \"name\": \"Ada\"\n}",
            DeveloperToolboxTools.formatJson("{\"name\":\"Ada\"}", true)
        )
        assertEquals("{\"name\":\"Ada\"}", DeveloperToolboxTools.formatJson("{ \"name\" : \"Ada\" }", false))
        assertThrows(IllegalArgumentException::class.java) { DeveloperToolboxTools.formatJson("{bad}") }
    }

    @Test
    fun xmlFormatterAcceptsWellFormedXmlAndRejectsDoctype() {
        val formatted = DeveloperToolboxTools.formatXml("<root><item>safe</item></root>")
        assertTrue(formatted.contains("<root>"))
        assertTrue(formatted.contains("<item>safe</item>"))
        assertThrows(Exception::class.java) {
            DeveloperToolboxTools.formatXml("""<!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><root>&xxe;</root>""")
        }
    }

    @Test
    fun textDiffShowsAddedAndRemovedLines() {
        val output = DeveloperToolboxTools.textDiff("one\ntwo", "one\nthree")
        assertTrue(output.contains("- two"))
        assertTrue(output.contains("+ three"))
        assertEquals("No differences.", DeveloperToolboxTools.textDiff("same", "same"))
    }

    @Test
    fun base64AndUrlEncodingRoundTrip() {
        val encoded = DeveloperToolboxTools.execute("base64", "Hello, world!", mode = "Encode")
        assertEquals("Hello, world!", DeveloperToolboxTools.execute("base64", encoded, mode = "Decode"))
        assertEquals("hello%20world%20%26%20x", DeveloperToolboxTools.execute("url", "hello world & x", mode = "Encode"))
        assertEquals("hello world & x", DeveloperToolboxTools.execute("url", "hello%20world%20%26%20x", mode = "Decode"))
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperToolboxTools.execute("base64", "not base64!", mode = "Decode")
        }
    }

    @Test
    fun jsonPointerHandlesEscapedKeysAndMissingValues() {
        val output = DeveloperToolboxTools.execute("json-pointer", """{"a/b":{"~key":42}}""", "/a~1b/~0key")
        assertTrue(output.contains("42"))
        assertTrue(output.contains("Type: number"))
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperToolboxTools.execute("json-pointer", """{"a":1}""", "/missing")
        }
    }

    @Test
    fun safeRegexTesterFindsGroupsAndRejectsUnsupportedBackreferences() {
        val output = DeveloperToolboxTools.execute("regex", """([A-Za-z]+)-(\d+)""", "id-42 name-7")
        assertTrue(output.contains("id-42"))
        assertTrue(output.contains("name-7"))
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperToolboxTools.execute("regex", """(a)\1""", "aa")
        }
    }

    @Test
    fun jwtInspectorExplicitlyWarnsClaimsAreUnverified() {
        fun part(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val jwt = part("""{"alg":"none"}""") + "." +
            part("""{"sub":"demo","exp":1893456000}""") + ".signature"
        val output = DeveloperToolboxTools.execute("jwt", jwt)
        assertTrue(output.contains("demo"))
        assertTrue(output.contains("trust validation"))
        assertTrue(output.contains("2030-01-01"))
    }

    @Test
    fun hashIsSha256AndNotEncryption() {
        val output = DeveloperToolboxTools.execute("hash", "hello", mode = "SHA-256")
        assertTrue(output.startsWith("SHA-256\n"))
        assertTrue(output.contains("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
    }

    @Test
    fun uuidGeneratorHasSafeBounds() {
        val values = DeveloperToolboxTools.execute("uuid", "5").lines()
        assertEquals(5, values.size)
        assertTrue(values.all { it.matches(Regex("""[0-9a-f-]{36}""")) })
        assertThrows(IllegalArgumentException::class.java) { DeveloperToolboxTools.execute("uuid", "101") }
    }

    @Test
    fun timestampAndContrastCalculations() {
        val timestamp = DeveloperToolboxTools.execute("timestamp", "1700000000")
        assertTrue(timestamp.contains("2023-11-14T22:13:20Z"))
        val contrast = DeveloperToolboxTools.execute("color", "#000000", "#FFFFFF")
        assertTrue(contrast.contains("21.00:1"))
        assertTrue(contrast.contains("WCAG AA normal text (4.5:1): pass"))
    }

    @Test
    fun uriInspectorRedactsSecretsAndDoesNotExecuteLinks() {
        val output = DeveloperToolboxTools.execute("uri", "https://example.com/path?token=secret&mode=demo&api_key=private")
        assertFalse(output.contains("secret"))
        assertFalse(output.contains("private"))
        assertTrue(output.contains("token=[REDACTED]"))
        assertTrue(output.contains("no request is sent"))
    }

    @Test
    fun bundledReferenceAndInputLimitAreHonest() {
        assertTrue(DeveloperToolboxTools.execute("http-status", "429").contains("Too Many Requests"))
        assertTrue(DeveloperToolboxTools.execute("mime", ".json").contains("application/json"))
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperToolboxTools.execute("text", "x".repeat(DeveloperToolboxTools.MAX_INPUT_CHARS + 1))
        }
        assertThrows(IllegalArgumentException::class.java) { DeveloperToolboxTools.execute("unsupported-tool", "") }
    }
}
