package com.vibesync.promptstudio.presentation.browser

import java.net.URI
import com.vibesync.promptstudio.R

/**
 * Curated website directory. Availability, pricing, and account requirements are controlled by
 * each provider and may change; this catalog does not promise that every service is free.
 */
enum class AiProvider(
    val titleRes: Int,
    val shortTitleRes: Int,
    val startUrl: String,
    val promptHosts: Set<String>,
    val icon: String
) {
    GEMINI(R.string.provider_gemini, R.string.provider_gemini_short, "https://gemini.google.com", setOf("gemini.google.com"), "✦"),
    CHATGPT(R.string.provider_chatgpt, R.string.provider_chatgpt_short, "https://chatgpt.com", setOf("chatgpt.com", "chat.openai.com"), "◎"),
    CLAUDE(R.string.provider_claude, R.string.provider_claude_short, "https://claude.ai", setOf("claude.ai"), "✳"),
    COPILOT(R.string.provider_copilot, R.string.provider_copilot_short, "https://copilot.microsoft.com", setOf("copilot.microsoft.com"), "◈"),
    DEEPSEEK(R.string.provider_deepseek, R.string.provider_deepseek_short, "https://chat.deepseek.com", setOf("chat.deepseek.com"), "◉"),
    MISTRAL(R.string.provider_mistral, R.string.provider_mistral_short, "https://chat.mistral.ai", setOf("chat.mistral.ai"), "M"),
    META_AI(R.string.provider_meta_ai, R.string.provider_meta_ai_short, "https://www.meta.ai", setOf("meta.ai"), "∞"),
    PERPLEXITY(R.string.provider_perplexity, R.string.provider_perplexity_short, "https://www.perplexity.ai", setOf("perplexity.ai"), "P"),
    HUGGINGCHAT(R.string.provider_huggingchat, R.string.provider_huggingchat_short, "https://huggingface.co/chat", setOf("huggingface.co"), "🤗"),
    DUCKDUCKGO(R.string.provider_duckduckgo, R.string.provider_duckduckgo_short, "https://duckduckgo.com/chat", setOf("duckduckgo.com"), "●"),
    POE(R.string.provider_poe, R.string.provider_poe_short, "https://poe.com", setOf("poe.com"), "P"),
    QWEN(R.string.provider_qwen, R.string.provider_qwen_short, "https://chat.qwenlm.ai", setOf("chat.qwenlm.ai"), "Q"),
    PHIND(R.string.provider_phind, R.string.provider_phind_short, "https://www.phind.com", setOf("phind.com"), "⌕"),
    CUSTOM(R.string.provider_custom, R.string.provider_custom_short, "https://example.com", emptySet(), "＋");

    fun supportsPromptInjection(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return promptHosts.any { host == it || host.endsWith(".$it") }
    }

    companion object {
        fun fromHost(url: String): AiProvider? {
            val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return null
            return entries.firstOrNull { it.promptHosts.any { suffix -> host == suffix || host.endsWith(".$suffix") } }
        }
    }
}
