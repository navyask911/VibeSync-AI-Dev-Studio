package com.vibesync.promptstudio.domain.usecase

import com.google.re2j.Pattern
import java.io.StringReader
import java.io.StringWriter
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.xml.sax.InputSource

/** Bounded offline utilities. XML external entities are disabled. JWT decoding never validates trust. */
object DeveloperToolboxTools {
    const val MAX_INPUT_CHARS = 100_000
    private val parser = Json { isLenient = false; ignoreUnknownKeys = false }
    private val pretty = Json { isLenient = false; prettyPrint = true; prettyPrintIndent = "  " }

    fun execute(id: String, input: String, second: String = "", mode: String = "Default"): String {
        require(input.length <= MAX_INPUT_CHARS && second.length <= MAX_INPUT_CHARS) {
            "Each input is limited to $MAX_INPUT_CHARS characters."
        }
        return when (id) {
            "json" -> formatJson(input, mode != "Minify")
            "xml" -> formatXml(input)
            "diff" -> textDiff(input, second)
            "base64" -> base64(input, mode)
            "url" -> urlCodec(input, mode)
            "json-pointer" -> jsonPointer(input, second)
            "regex" -> regexTester(input, second)
            "jwt" -> inspectJwt(input)
            "hash" -> hash(input, mode)
            "uuid" -> generateUuids(input)
            "timestamp" -> convertTimestamp(input)
            "color" -> contrast(input, second)
            "unicode" -> unicode(input)
            "text" -> transform(input, mode)
            "http-status" -> httpStatus(input)
            "mime" -> mimeType(input)
            "uri" -> inspectUri(input)
            else -> throw IllegalArgumentException("Choose a tool from the catalogue.")
        }
    }

    fun formatJson(input: String, isPretty: Boolean = true): String {
        require(input.isNotBlank()) { "Enter JSON to format." }
        val value = parser.parseToJsonElement(input)
        return if (isPretty) pretty.encodeToString(JsonElement.serializer(), value)
        else parser.encodeToString(JsonElement.serializer(), value)
    }

    fun formatXml(input: String): String {
        require(input.isNotBlank()) { "Enter XML to format." }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(input)))
        val transformFactory = TransformerFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "") }
        }
        val writer = StringWriter()
        transformFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            runCatching { setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2") }
        }.transform(DOMSource(document), StreamResult(writer))
        return writer.toString()
    }

    fun textDiff(before: String, after: String): String {
        val a = before.lines()
        val b = after.lines()
        require(a.size <= 500 && b.size <= 500) { "Diff is limited to 500 lines per input." }
        if (a == b) return "No differences."
        return buildString {
            appendLine("--- Original")
            appendLine("+++ Updated")
            for (i in 0 until maxOf(a.size, b.size)) {
                val left = a.getOrNull(i)
                val right = b.getOrNull(i)
                if (left == right && left != null) appendLine("  $left")
                else {
                    left?.let { appendLine("- $it") }
                    right?.let { appendLine("+ $it") }
                }
            }
            appendLine()
            appendLine("Note: bounded line-by-line diff; inserted lines may shift later rows.")
        }
    }

    private fun base64(input: String, mode: String): String {
        if (mode != "Decode") return Base64.getEncoder().encodeToString(input.toByteArray(StandardCharsets.UTF_8))
        val text = input.filterNot(Char::isWhitespace)
        val bytes = runCatching { Base64.getDecoder().decode(text) }
            .recoverCatching { Base64.getUrlDecoder().decode(text) }
            .getOrElse { throw IllegalArgumentException("Input is not valid standard or URL-safe Base64.") }
        return bytes.toString(StandardCharsets.UTF_8)
    }

    private fun urlCodec(input: String, mode: String): String =
        if (mode == "Decode") runCatching { URLDecoder.decode(input, "UTF-8") }
            .getOrElse { throw IllegalArgumentException("Malformed URL-encoded input.") }
        else URLEncoder.encode(input, "UTF-8").replace("+", "%20")

    private fun jsonPointer(document: String, pointer: String): String {
        require(document.isNotBlank()) { "Enter the JSON document in the first field." }
        require(pointer.isEmpty() || pointer.startsWith("/")) { "JSON Pointer must be empty (root) or start with '/'." }
        var value = parser.parseToJsonElement(document)
        for (segment in pointer.removePrefix("/").let { if (pointer.isEmpty()) emptyList() else it.split('/') }
            .map { it.replace("~1", "/").replace("~0", "~") }) {
            value = when (value) {
                is JsonObject -> value[segment] ?: throw IllegalArgumentException("No object property named '$segment'.")
                is JsonArray -> value.getOrNull(segment.toIntOrNull() ?: -1)
                    ?: throw IllegalArgumentException("Array index '$segment' is invalid or out of range.")
                else -> throw IllegalArgumentException("Cannot traverse into a scalar at '$segment'.")
            }
        }
        return "Type: " + jsonType(value) + "\n\n" + pretty.encodeToString(JsonElement.serializer(), value)
    }

    private fun regexTester(expression: String, text: String): String {
        require(expression.isNotBlank()) { "Enter an expression in the first field." }
        require(expression.length <= 256 && text.length <= 20_000) { "Limit regex to 256 characters and test text to 20,000 characters." }
        val pattern = try { Pattern.compile(expression) }
        catch (e: Exception) { throw IllegalArgumentException("Invalid or unsupported safe-regex syntax: " + e.message.orEmpty().take(120)) }
        val matcher = pattern.matcher(text)
        val out = mutableListOf<String>()
        var matchCount = 0
        while (matcher.find() && matchCount < 100) {
            matchCount++
            out += "Match $matchCount [${matcher.start()}, ${matcher.end()}): ${matcher.group()}"
            for (group in 1..matcher.groupCount()) {
                out += "  Group $group: " + (matcher.group(group) ?: "(unmatched)")
            }
        }
        return (if (out.isEmpty()) "No matches." else out.joinToString("\n")) +
            "\n\nUses RE2/J's linear-time engine; look-around and backreferences are not supported. Matches are capped at 100."
    }

    private fun inspectJwt(input: String): String {
        val parts = input.trim().split('.')
        require(parts.size == 3 && parts.all { it.isNotBlank() }) { "Compact JWT should contain three dot-separated parts." }
        fun decode(part: String, name: String) = runCatching {
            Base64.getUrlDecoder().decode(part).toString(StandardCharsets.UTF_8)
        }.getOrElse { throw IllegalArgumentException("JWT $name is not valid base64url.") }
        val header = pretty.encodeToString(JsonElement.serializer(), parser.parseToJsonElement(decode(parts[0], "header")))
        val claims = parser.parseToJsonElement(decode(parts[1], "payload"))
        return buildString {
            appendLine("Decoded header (unverified)")
            appendLine(header)
            appendLine()
            appendLine("Decoded payload (unverified)")
            appendLine(pretty.encodeToString(JsonElement.serializer(), claims))
            (claims as? JsonObject)?.get("exp")?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.let {
                appendLine()
                appendLine("Expiry: " + runCatching { Instant.ofEpochSecond(it).toString() }.getOrDefault("invalid exp value"))
            }
            appendLine()
            appendLine("WARNING: decoding is not signature, issuer, audience, or trust validation. Never use decoded claims alone for authorization.")
        }
    }

    private fun hash(input: String, mode: String): String {
        val algorithm = if (mode == "SHA-512") "SHA-512" else "SHA-256"
        val digest = MessageDigest.getInstance(algorithm).digest(input.toByteArray(StandardCharsets.UTF_8))
        return "$algorithm\n" + digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateUuids(input: String): String {
        val count = input.trim().toIntOrNull() ?: 1
        require(count in 1..100) { "Generate between 1 and 100 UUIDs." }
        return List(count) { UUID.randomUUID().toString() }.joinToString("\n")
    }

    private fun convertTimestamp(input: String): String {
        val value = input.trim()
        require(value.isNotBlank()) { "Enter epoch seconds, milliseconds, or ISO-8601 UTC timestamp." }
        val instant = if (value.matches(Regex("""-?\d{1,17}"""))) {
            val number = value.toLongOrNull() ?: throw IllegalArgumentException("Timestamp is outside the supported range.")
            if (value.trimStart('-').length <= 10) Instant.ofEpochSecond(number) else Instant.ofEpochMilli(number)
        } else runCatching { Instant.parse(value) }
            .getOrElse { throw IllegalArgumentException("Use epoch seconds/milliseconds or an ISO-8601 timestamp (for example 2026-01-02T03:04:05Z).") }
        return "UTC ISO-8601: $instant\nEpoch seconds: ${instant.epochSecond}\nEpoch milliseconds: ${instant.toEpochMilli()}"
    }

    private fun contrast(foreground: String, background: String): String {
        fun parse(raw: String): Int {
            val s = raw.trim().removePrefix("#")
            val expanded = when (s.length) {
                3 -> s.map { "$it$it" }.joinToString("")
                6 -> s
                else -> throw IllegalArgumentException("Use a 3- or 6-digit HEX color such as #112233.")
            }
            return expanded.toIntOrNull(16) ?: throw IllegalArgumentException("HEX color contains invalid characters.")
        }
        fun luminance(rgb: Int): Double {
            fun ch(shift: Int): Double {
                val c = ((rgb shr shift) and 255) / 255.0
                return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return .2126 * ch(16) + .7152 * ch(8) + .0722 * ch(0)
        }
        val l1 = luminance(parse(foreground))
        val l2 = luminance(parse(background))
        val ratio = (maxOf(l1, l2) + .05) / (minOf(l1, l2) + .05)
        return "Contrast ratio: %.2f:1".format(Locale.ROOT, ratio) +
            "\nWCAG AA normal text (4.5:1): " + (if (ratio >= 4.5) "pass" else "fail") +
            "\nWCAG AA large text (3:1): " + (if (ratio >= 3.0) "pass" else "fail")
    }

    private fun unicode(input: String): String = buildString {
        appendLine("UTF-16 code units: ${input.length}")
        appendLine("Unicode code points: ${input.codePointCount(0, input.length)}")
        appendLine("UTF-8 bytes: ${input.toByteArray(StandardCharsets.UTF_8).size}")
        appendLine()
        var offset = 0
        var index = 0
        while (offset < input.length && index < 2_000) {
            val cp = input.codePointAt(offset)
            val item = String(Character.toChars(cp))
            append("[").append(index).append("] ")
                .append(String.format(Locale.ROOT, "U+%04X", cp)).append(" ")
                .append(if (item.all(Char::isWhitespace)) "(whitespace)" else "'$item'").appendLine()
            offset += Character.charCount(cp)
            index++
        }
        if (offset < input.length) appendLine("Output truncated at 2,000 code points.")
        appendLine()
        appendLine("Detected line endings: " + when {
            input.contains("\r\n") && !input.replace("\r\n", "").contains('\r') && !input.replace("\r\n", "").contains('\n') -> "CRLF"
            input.contains('\r') && !input.contains('\n') -> "CR"
            input.contains('\n') && !input.contains('\r') -> "LF"
            else -> "mixed or none"
        })
    }

    private fun transform(input: String, mode: String): String = when (mode) {
        "UPPERCASE" -> input.uppercase(Locale.ROOT)
        "lowercase" -> input.lowercase(Locale.ROOT)
        "Trim" -> input.trim()
        "Sort lines" -> input.lines().sorted().joinToString("\n")
        "Deduplicate lines" -> input.lines().distinct().joinToString("\n")
        "Escape JSON string" -> JsonPrimitive(input).toString()
        "Unescape JSON string" -> (parser.parseToJsonElement(input) as? JsonPrimitive)?.contentOrNull
            ?: throw IllegalArgumentException("Enter a JSON string literal.")
        else -> input
    }

    private data class Status(val code: Int, val name: String, val detail: String)
    private val statuses = listOf(
        Status(200,"OK","The request succeeded."), Status(201,"Created","A resource was created."),
        Status(202,"Accepted","Accepted for processing."), Status(204,"No Content","Success with no body."),
        Status(301,"Moved Permanently","Permanent redirect; update the stored URL."),
        Status(302,"Found","Temporary redirect; check redirect handling."),
        Status(304,"Not Modified","The cached representation remains valid."),
        Status(400,"Bad Request","The request syntax or parameters are invalid."),
        Status(401,"Unauthorized","Authentication is missing or invalid."),
        Status(403,"Forbidden","The server denied access."),
        Status(404,"Not Found","The resource was not found."),
        Status(405,"Method Not Allowed","The method is unsupported for this resource."),
        Status(408,"Request Timeout","The server timed out waiting for the request."),
        Status(409,"Conflict","The request conflicts with current resource state."),
        Status(413,"Content Too Large","Request body exceeds server limit."),
        Status(415,"Unsupported Media Type","The content type is unsupported."),
        Status(422,"Unprocessable Content","The request was understood but not processable."),
        Status(429,"Too Many Requests","Honor Retry-After when present."),
        Status(500,"Internal Server Error","Unexpected server condition."),
        Status(502,"Bad Gateway","An upstream server returned an invalid response."),
        Status(503,"Service Unavailable","The service is temporarily unavailable."),
        Status(504,"Gateway Timeout","An upstream server timed out.")
    )

    private fun httpStatus(query: String): String {
        require(query.isNotBlank()) { "Enter a status code or keyword (such as 429 or timeout)." }
        val code = query.trim().toIntOrNull()
        val results = if (code != null) statuses.filter { it.code == code }
        else statuses.filter { (it.name + " " + it.detail).contains(query.trim(), true) }
        return if (results.isEmpty()) "No match in this compact bundled reference."
        else results.joinToString("\n\n") { "${it.code} ${it.name}\n${it.detail}" } +
            "\n\nThis compact reference is not exhaustive."
    }

    private fun mimeType(query: String): String {
        require(query.isNotBlank()) { "Enter an extension (e.g. .json) or MIME type." }
        val items = listOf(
            "json" to "application/json", "xml" to "application/xml", "txt" to "text/plain",
            "html" to "text/html", "css" to "text/css", "js" to "text/javascript",
            "pdf" to "application/pdf", "zip" to "application/zip", "png" to "image/png",
            "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "webp" to "image/webp",
            "svg" to "image/svg+xml", "mp3" to "audio/mpeg", "mp4" to "video/mp4",
            "csv" to "text/csv", "yaml" to "application/yaml", "yml" to "application/yaml",
            "wasm" to "application/wasm"
        )
        val key = query.trim().removePrefix(".")
        val results = if (key.contains('/')) items.filter { it.second.equals(key, true) }
        else items.filter { it.first.equals(key, true) }
        return if (results.isEmpty()) "No match in this compact bundled reference; mappings can be ambiguous."
        else results.joinToString("\n") { ".${it.first} → ${it.second}" } + "\n\nReference is compact and not exhaustive."
    }

    private fun inspectUri(input: String): String {
        require(input.isNotBlank()) { "Enter a URI to inspect." }
        val uri = runCatching { java.net.URI(input.trim()) }
            .getOrElse { throw IllegalArgumentException("Malformed URI. Use a complete URI such as https://example.com/path?q=1.") }
        val query = uri.rawQuery.orEmpty().takeIf { it.isNotBlank() }?.split('&')?.joinToString("&") { part ->
            val key = part.substringBefore('=')
            if (key.lowercase(Locale.ROOT) in setOf("key","api_key","token","access_token","refresh_token","code","state","client_secret","password"))
                "$key=[REDACTED]" else part
        } ?: "(none)"
        return "Scheme: ${uri.scheme ?: "(relative URI)"}\nHost: ${uri.host ?: "(none)"}\nPath: ${uri.rawPath.orEmpty()}\nQuery: $query\nFragment: ${uri.rawFragment ?: "(none)"}\n\nThis is a local URI preview only; no link is launched and no request is sent."
    }

    private fun jsonType(value: JsonElement): String = when (value) {
        is JsonObject -> "object"
        is JsonArray -> "array"
        is JsonPrimitive -> when {
            value.isString -> "string"
            value.booleanOrNull != null -> "boolean"
            value.content == "null" -> "null"
            value.content.toDoubleOrNull() != null -> "number"
            else -> "primitive"
        }
    }
}
