package com.vibesync.promptstudio.data.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.system.exitProcess

enum class DiagnosticSeverity { VERBOSE, DEBUG, INFO, WARNING, ERROR }

data class DiagnosticRecord(
    val timestampMillis: Long,
    val severity: DiagnosticSeverity,
    val component: String,
    val category: String,
    val operation: String,
    val provider: String?,
    val message: String,
    val processId: Int,
    val appVersion: String
)

object DiagnosticRedactor {
    private val labeledSecret = Regex("""(?i)(authorization|x-api-key|x-goog-api-key|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|code_verifier|code_challenge)\s*[:=]\s*["']?(?:bearer\s+)?[^,\s;"']+""")
    private val bearerToken = Regex("""(?i)\bbearer\s+[A-Za-z0-9._~+/-]+=*""")
    private val openAiKey = Regex("""\bsk-(?:ant-)?[A-Za-z0-9_-]{12,}\b""")
    private val googleKey = Regex("""\bAIza[0-9A-Za-z_-]{20,}\b""")
    private val querySecret = Regex("""(?i)([?&](?:key|token|access_token|api_key|code|state)=)[^&\s]+""")

    fun redact(input: String): String {
        var value = input
        value = labeledSecret.replace(value) { it.groupValues[1] + "=[REDACTED]" }
        value = bearerToken.replace(value, "Bearer [REDACTED]")
        value = openAiKey.replace(value, "[REDACTED_PROVIDER_KEY]")
        value = googleKey.replace(value, "[REDACTED_PROVIDER_KEY]")
        value = querySecret.replace(value) { it.groupValues[1] + "[REDACTED]" }
        return value
    }
}

class BoundedDiagnosticBuffer(private val capacity: Int) {
    private val entries = mutableListOf<DiagnosticRecord>()

    init { require(capacity > 0) }

    @Synchronized
    fun add(record: DiagnosticRecord) {
        entries.add(record)
        while (entries.size > capacity) entries.removeAt(0)
    }

    @Synchronized
    fun snapshot(): List<DiagnosticRecord> = entries.toList()

    @Synchronized
    fun clear() = entries.clear()
}

class DiagnosticsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val buffer = BoundedDiagnosticBuffer(MAX_RECORDS)

    init {
        runCatching {
            val stored = JSONArray(preferences.getString(KEY_RECORDS, "[]"))
            for (index in 0 until stored.length()) {
                val item = stored.optJSONObject(index) ?: continue
                buffer.add(item.toRecord())
            }
        }
    }

    @Synchronized
    fun record(
        severity: DiagnosticSeverity,
        component: String,
        category: String,
        operation: String,
        message: String,
        provider: String? = null,
        durable: Boolean = false
    ) {
        val record = DiagnosticRecord(
            timestampMillis = System.currentTimeMillis(),
            severity = severity,
            component = DiagnosticRedactor.redact(component).take(64),
            category = DiagnosticRedactor.redact(category).take(64),
            operation = DiagnosticRedactor.redact(operation).take(80),
            provider = provider?.let { DiagnosticRedactor.redact(it).take(40) },
            message = DiagnosticRedactor.redact(message).take(MAX_MESSAGE_CHARS),
            processId = Process.myPid(),
            appVersion = appVersion()
        )
        buffer.add(record)
        if (provider != null) preferences.edit().putString(KEY_LAST_PROVIDER, DiagnosticRedactor.redact(provider).take(40)).apply()
        val encoded = JSONArray()
        buffer.snapshot().forEach { encoded.put(it.toJson()) }
        val edit = preferences.edit().putString(KEY_RECORDS, encoded.toString())
        if (durable) edit.commit() else edit.apply()
    }

    fun recordSuccessfulPromptRun(provider: String) {
        preferences.edit().putInt(KEY_SUCCESSFUL_RUNS, successfulPromptRuns() + 1).apply()
        record(
            DiagnosticSeverity.INFO,
            "prompt",
            "usage",
            "run_succeeded",
            "Prompt run completed successfully; prompt and response content omitted.",
            provider
        )
    }

    fun successfulPromptRuns(): Int = preferences.getInt(KEY_SUCCESSFUL_RUNS, 0)

    fun clear() {
        buffer.clear()
        preferences.edit().remove(KEY_RECORDS).apply()
    }

    @Synchronized
    fun logs(
        severity: DiagnosticSeverity? = null,
        query: String = "",
        component: String = "",
        provider: String = "",
        category: String = "",
        processId: Int? = null,
        fromTimestampMillis: Long? = null,
        toTimestampMillis: Long? = null
    ): List<DiagnosticRecord> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val tag = component.trim().lowercase(Locale.ROOT)
        val providerNeedle = provider.trim().lowercase(Locale.ROOT)
        val categoryNeedle = category.trim().lowercase(Locale.ROOT)
        return buffer.snapshot().asReversed().filter { record ->
            (severity == null || record.severity == severity) &&
                (needle.isEmpty() || listOf(record.message, record.category, record.operation, record.provider.orEmpty(), record.component)
                    .any { it.lowercase(Locale.ROOT).contains(needle) }) &&
                (tag.isEmpty() || record.component.lowercase(Locale.ROOT).contains(tag)) &&
                (providerNeedle.isEmpty() || record.provider.orEmpty().lowercase(Locale.ROOT).contains(providerNeedle)) &&
                (categoryNeedle.isEmpty() || (record.category + " " + record.operation).lowercase(Locale.ROOT).contains(categoryNeedle)) &&
                (processId == null || record.processId == processId) &&
                (fromTimestampMillis == null || record.timestampMillis >= fromTimestampMillis) &&
                (toTimestampMillis == null || record.timestampMillis <= toTimestampMillis)
        }
    }

    fun environmentSummary(): List<String> {
        val exits = processExitSummaries()
        return listOf(
            "App: " + appVersion() + " (version code " + appVersionCode() + ")",
            "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
            "Device: " + Build.MANUFACTURER.orEmpty() + " " + Build.MODEL.orEmpty(),
            "Process ID: " + Process.myPid(),
            "Selected provider: " + preferences.getString(KEY_LAST_PROVIDER, "not recorded"),
            "Captured: " + formatTime(System.currentTimeMillis()),
            "Diagnostic source: app-collected records only; this is not system-wide Logcat.",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Historical process exits shown below are supplied by Android." else "Historical process-exit information requires Android 11 (API 30) or newer."
        ) + exits.take(8)
    }

    fun processExitSummaries(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return runCatching {
            val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 8).map { info ->
                "Process exit: " + formatTime(info.timestamp) +
                    " • " + exitReason(info.reason) +
                    " • pid=" + info.pid +
                    " • status=" + info.status +
                    ""
            }
        }.getOrElse { listOf("Historical process-exit lookup failed: " + DiagnosticRedactor.redact(it.javaClass.simpleName)) }
    }

    fun report(selected: List<DiagnosticRecord> = logs()): String = buildReport(environmentSummary(), selected)

    fun exportReport(selected: List<DiagnosticRecord> = logs()): File {
        val directory = File(appContext.filesDir, "diagnostics/exports")
        check(directory.mkdirs() || directory.isDirectory) { "Could not create the diagnostic export directory." }
        val file = File(directory, "vibesync-diagnostics-" + System.currentTimeMillis() + ".txt")
        file.writeText(report(selected), Charsets.UTF_8)
        directory.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_EXPORT_FILES)?.forEach { runCatching { it.delete() } }
        return file
    }

    private fun appVersion(): String = runCatching { packageInfo().versionName ?: "unknown" }.getOrDefault("unknown")
    private fun appVersionCode(): Long = runCatching { packageInfo().let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong() } }.getOrDefault(-1L)

    @Suppress("DEPRECATION")
    private fun packageInfo(): PackageInfo = if (Build.VERSION.SDK_INT >= 33) {
        appContext.packageManager.getPackageInfo(appContext.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
    } else {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    }

    private fun exitReason(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "unknown"
        ApplicationExitInfo.REASON_EXIT_SELF -> "self-exit"
        ApplicationExitInfo.REASON_SIGNALED -> "signaled"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low-memory"
        ApplicationExitInfo.REASON_CRASH -> "Java crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency died"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource use"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "user requested"
        ApplicationExitInfo.REASON_USER_STOPPED -> "user stopped"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization failure"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission change"
        ApplicationExitInfo.REASON_OTHER -> "other"
        ApplicationExitInfo.REASON_FREEZER -> "freezer"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "package state change"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "package updated"
        else -> "reason " + reason
    }

    private fun formatTime(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(time))

    private fun DiagnosticRecord.toJson(): JSONObject = JSONObject()
        .put("time", timestampMillis)
        .put("severity", severity.name)
        .put("component", component)
        .put("category", category)
        .put("operation", operation)
        .put("provider", provider)
        .put("message", message)
        .put("pid", processId)
        .put("version", appVersion)

    private fun JSONObject.toRecord(): DiagnosticRecord = DiagnosticRecord(
        timestampMillis = optLong("time", System.currentTimeMillis()),
        severity = runCatching { DiagnosticSeverity.valueOf(optString("severity", "INFO")) }.getOrDefault(DiagnosticSeverity.INFO),
        component = optString("component", "app"),
        category = optString("category", "general"),
        operation = optString("operation", "event"),
        provider = optString("provider").takeIf { it.isNotBlank() && it != "null" },
        message = DiagnosticRedactor.redact(optString("message", "")).take(MAX_MESSAGE_CHARS),
        processId = optInt("pid", -1),
        appVersion = optString("version", "unknown")
    )

    private fun sanitizedStackTrace(error: Throwable): String = buildString {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 5) {
            append(current.javaClass.name.take(160)).append('\n')
            current.stackTrace.take(24).forEach { frame ->
                append(" at ")
                    .append(frame.className.take(180)).append('.')
                    .append(frame.methodName.take(100))
                    .append('(').append(frame.fileName?.take(100) ?: "Unknown Source")
                    .append(if (frame.lineNumber >= 0) ":" + frame.lineNumber else "")
                    .appendLine(")")
            }
            current = current.cause
            depth++
        }
    }.take(12_000)

    companion object {
        private const val PREFERENCES_NAME = "vibesync_diagnostics_v1"
        private const val KEY_RECORDS = "records"
        private const val KEY_LAST_PROVIDER = "last_provider"
        private const val KEY_SUCCESSFUL_RUNS = "successful_prompt_runs"
        private const val MAX_RECORDS = 250
        private const val MAX_MESSAGE_CHARS = 1_200
        private const val MAX_EXPORT_FILES = 3

        fun buildReport(environment: List<String>, records: List<DiagnosticRecord>): String = buildString {
            appendLine("VibeSync Prompt Studio — Diagnostic Report")
            appendLine("Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()))
            appendLine()
            appendLine("Environment")
            environment.forEach { appendLine(DiagnosticRedactor.redact(it)) }
            appendLine()
            appendLine("App-collected diagnostics only (not device-wide Logcat)")
            records.forEach { record ->
                appendLine(
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(record.timestampMillis)) +
                        " [" + record.severity.name + "] " + record.component + "/" + record.category +
                        " op=" + record.operation +
                        (record.provider?.let { " provider=" + it } ?: "") +
                        " pid=" + record.processId + " — " + DiagnosticRedactor.redact(record.message)
                )
            }
            appendLine()
            appendLine("Prompt contents, model outputs, API keys, and authorization headers are not intentionally captured.")
            appendLine("Full system Logcat is restricted for ordinary apps. Use Android Studio/ADB from a development computer for device-wide diagnostics.")
        }
    }
}

class DiagnosticUncaughtExceptionHandler(
    private val store: DiagnosticsStore,
    private val delegate: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        runCatching {
            store.record(
                DiagnosticSeverity.ERROR,
                "crash",
                "process",
                "uncaught_exception",
                "Uncaught exception on thread " + DiagnosticRedactor.redact(thread.name).take(80) + ":\n" + sanitizedStackTrace(error),
                durable = true
            )
        }
        if (delegate != null && delegate !== this) {
            delegate.uncaughtException(thread, error)
        } else {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
