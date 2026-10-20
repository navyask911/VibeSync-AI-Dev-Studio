package com.vibesync.promptstudio.data.remote

import com.vibesync.promptstudio.domain.model.PromptFailure
import com.vibesync.promptstudio.domain.model.PromptRequest
import com.vibesync.promptstudio.domain.model.ProviderType
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.contentType
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.SocketTimeoutException

class KtorProviderStreamingClient {
    val httpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = false })
        }
        engine {
            config {
                retryOnConnectionFailure(true)
                followRedirects(false)
            }
        }
    }

    fun stream(request: PromptRequest, apiKey: String): Flow<String> = flow {
        require(apiKey.isNotBlank()) { "API key is blank." }
        val endpoint = endpointFor(request)
        val payload = bodyFor(request)
        try {
            httpClient.preparePost(endpoint) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "text/event-stream")
                when (request.config.provider) {
                    ProviderType.OPENAI -> header(HttpHeaders.Authorization, "Bearer " + apiKey)
                    ProviderType.ANTHROPIC -> {
                        header("x-api-key", apiKey)
                        header("anthropic-version", "2023-06-01")
                    }
                    ProviderType.GEMINI -> header("x-goog-api-key", apiKey)
                }
                setBody(payload.toString())
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText().take(MAX_ERROR_BODY_CHARS)
                    val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                    when (response.status.value) {
                        401, 403 -> throw PromptFailure.Unauthorized()
                        429 -> throw PromptFailure.RateLimited(retryAfter)
                        else -> throw PromptFailure.ProviderError(providerErrorMessage(request.config.provider, response.status.value, errorBody))
                    }
                }

                val channel = response.bodyAsChannel()
                val dataLines = mutableListOf<String>()
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val line = channel.readUTF8Line() ?: break
                    when {
                        line.isEmpty() -> {
                            consumeEvent(request.config.provider, dataLines)?.let { emit(it) }
                            dataLines.clear()
                        }
                        line.startsWith("data:") -> dataLines += line.substringAfter("data:").trimStart()
                        line.startsWith(":") -> Unit
                    }
                }
                consumeEvent(request.config.provider, dataLines)?.let { emit(it) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: PromptFailure) {
            throw failure
        } catch (timeout: HttpRequestTimeoutException) {
            throw PromptFailure.Timeout(timeout)
        } catch (timeout: SocketTimeoutException) {
            throw PromptFailure.Timeout(timeout)
        } catch (io: IOException) {
            throw PromptFailure.Network(io)
        } catch (error: Exception) {
            throw PromptFailure.ProviderError("Could not parse the provider response. It may have been interrupted or malformed.", error)
        }
    }

    private fun endpointFor(request: PromptRequest): String = when (request.config.provider) {
        ProviderType.OPENAI -> "https://api.openai.com/v1/chat/completions"
        ProviderType.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
        ProviderType.GEMINI -> {
            val model = request.config.modelId
            if (!model.matches(Regex("""[A-Za-z0-9._-]{1,120}"""))) throw PromptFailure.InvalidInput("Invalid Gemini model identifier.")
            "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":streamGenerateContent?alt=sse"
        }
    }

    private fun bodyFor(request: PromptRequest): JsonObject = when (request.config.provider) {
        ProviderType.OPENAI -> buildJsonObject {
            put("model", JsonPrimitive(request.config.modelId))
            put("stream", JsonPrimitive(true))
            val reasoningModel = request.config.modelId.startsWith("o1") || request.config.modelId.startsWith("o3")
            put(if (reasoningModel) "max_completion_tokens" else "max_tokens", JsonPrimitive(request.config.maxOutputTokens))
            put("messages", buildJsonArray {
                if (request.systemPrompt.isNotBlank()) {
                    add(buildJsonObject {
                        put("role", JsonPrimitive(if (reasoningModel) "developer" else "system"))
                        put("content", JsonPrimitive(request.systemPrompt))
                    })
                }
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(request.userPrompt))
                })
            })
        }
        ProviderType.ANTHROPIC -> buildJsonObject {
            put("model", JsonPrimitive(request.config.modelId))
            put("max_tokens", JsonPrimitive(request.config.maxOutputTokens))
            put("stream", JsonPrimitive(true))
            if (request.systemPrompt.isNotBlank()) put("system", JsonPrimitive(request.systemPrompt))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(request.userPrompt))
                })
            })
        }
        ProviderType.GEMINI -> buildJsonObject {
            put("generationConfig", buildJsonObject { put("maxOutputTokens", JsonPrimitive(request.config.maxOutputTokens)) })
            if (request.systemPrompt.isNotBlank()) {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", JsonPrimitive(request.systemPrompt)) })
                    })
                })
            }
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", JsonPrimitive(request.userPrompt)) })
                    })
                })
            })
        }
    }

    private fun consumeEvent(provider: ProviderType, dataLines: List<String>): String? {
        if (dataLines.isEmpty()) return null
        val data = dataLines.joinToString("\n")
        if (data.isBlank() || data == "[DONE]") return null
        val root = try {
            Json.parseToJsonElement(data)
        } catch (error: Exception) {
            throw PromptFailure.ProviderError("The provider sent malformed streaming JSON.", error)
        }
        if (root is JsonObject && root["error"] != null) {
            throw PromptFailure.ProviderError("The provider returned an error: " + root["error"].toString().take(MAX_ERROR_BODY_CHARS))
        }
        return when (provider) {
            ProviderType.OPENAI -> {
                val type = (root as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
                if (type == "error") throw PromptFailure.ProviderError("OpenAI returned a streaming error: " + data.take(MAX_ERROR_BODY_CHARS))
                (root as? JsonObject)?.get("choices")?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            }
            ProviderType.ANTHROPIC -> {
                val type = (root as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
                if (type == "error") throw PromptFailure.ProviderError("Anthropic returned a streaming error: " + data.take(MAX_ERROR_BODY_CHARS))
                if (type == "content_block_delta") {
                    (root as? JsonObject)?.get("delta")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                } else null
            }
            ProviderType.GEMINI -> extractGeminiText(root)
        }
    }

    private fun extractGeminiText(element: JsonElement): String? {
        val candidates: JsonArray? = when (element) {
            is JsonArray -> element
            is JsonObject -> if (element["candidates"] != null) JsonArray(listOf(element)) else null
            else -> null
        }
        return candidates?.flatMap { candidate ->
            val candidateObject = candidate.jsonObject
            val nested = candidateObject["candidates"]?.jsonArray
            if (nested != null) {
                nested.flatMap { inner ->
                    inner.jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray?.mapNotNull { part ->
                        part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    }.orEmpty()
                }
            } else {
                candidateObject["content"]?.jsonObject?.get("parts")?.jsonArray?.mapNotNull { part ->
                    part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }.orEmpty()
            }
        }?.joinToString("")
    }

    private fun providerErrorMessage(provider: ProviderType, statusCode: Int, body: String): String {
        val label = when (provider) {
            ProviderType.OPENAI -> "OpenAI"
            ProviderType.ANTHROPIC -> "Anthropic"
            ProviderType.GEMINI -> "Google Gemini"
        }
        return when {
            statusCode in 400..499 -> "$label rejected the request (HTTP $statusCode). Check the model ID, API key, and usage limits."
            statusCode >= 500 -> "$label is temporarily unavailable (HTTP $statusCode). Try again later."
            else -> "$label returned HTTP $statusCode." + if (body.isNotBlank()) " " + body.take(160) else ""
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 180_000L
        private const val SOCKET_TIMEOUT_MS = 180_000L
        private const val MAX_ERROR_BODY_CHARS = 1_200
    }
}
