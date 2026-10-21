
<!--
  ╔══════════════════════════════════════════════════════════════╗
  ║  VibeSync AI Dev Studio — README                             ║
  ║  Universal AI Multi-Browser · Developer Studio · Privacy-First║
  ╚══════════════════════════════════════════════════════════════╝
-->

<div align="center">

<!-- 🎨 Replace with your actual logo asset (upload to /assets/logo.png) -->
<img src="assets/logo.png" alt="VibeSync AI Dev Studio Logo" width="160" />

# ⚡ VibeSync AI Dev Studio

**Universal AI Multi‑Browser Shell · Developer API Studio · Privacy‑First Prompt Workspace**

<p>
  <img src="https://img.shields.io/badge/Android-API%2036-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-8B5CF6?style=for-the-badge&logo=diagram&logoColor=white" />
</p>

<p>
  <img src="https://img.shields.io/badge/Security-AES%2FGCM%20Keystore%20Vault-E0115F?style=for-the-badge&logo=letsencrypt&logoColor=white" />
  <img src="https://img.shields.io/badge/Privacy-Zero%20Telemetry-DC143C?style=for-the-badge&logo=shield&logoColor=white" />
  <img src="https://img.shields.io/badge/Streaming-SSE%20Multi--Provider-0F52BA?style=for-the-badge&logo=openai&logoColor=white" />
  <img src="https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" />
</p>

<p>
  <img src="https://img.shields.io/badge/Localization-EN%20·%20ES%20·%20DE%20·%20HI%20·%20AR-FFBF00?style=flat-square&logo=googletranslate&logoColor=black" />
  <img src="https://img.shields.io/badge/Offline%20Toolbox-20%2B%20Utilities-10B981?style=flat-square&logo=gnubash&logoColor=white" />
  <img src="https://img.shields.io/badge/License-Proprietary-lightgrey?style=flat-square&logo=opensourceinitiative&logoColor=white" />
</p>

<p>
  <a href="#-why-vibesync">Why VibeSync</a> ·
  <a href="#-dual-mode-architecture">Dual Mode</a> ·
  <a href="#-feature-matrix">Features</a> ·
  <a href="#-security--privacy">Security</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-build--test">Build & Test</a> ·
  <a href="#-license">License</a>
</p>

</div>

---

## 🎨 Brand Palette

| Color | Hex | Usage |
|-------|-----|-------|
| 🔴 **Crimson** | `#DC143C` | Privacy, zero‑telemetry badges |
| 💖 **Ruby** | `#E0115F` | Security vault, Keystore accents |
| 🟢 **Emerald** | `#10B981` | Success, offline toolbox, build‑passing |
| 🔵 **Sapphire** | `#0F52BA` | Streaming, provider integrations |
| 🟡 **Amber** | `#FFBF00` | Localization, metadata, CI |
| 🟣 **Violet** | `#8B5CF6` | Kotlin, Compose, architecture |

---

## 🚀 Why VibeSync?

> **VibeSync AI Dev Studio** is an advanced, dual‑mode native Android workspace that unifies **worldwide conversational AI engines** into a high‑performance, single‑app experience—without compromising user privacy.

> [!IMPORTANT]
> **Strict Privacy‑First, Zero‑Telemetry Mandate**
> All prompts, web session cookies, API tokens, and local utilities remain entirely confined to the device. No analytics, no crash‑reporting SDK, no remote diagnostics.

---

## 🧠 Dual‑Mode Architecture

### 🌐 Mode 1 — Universal “AI Multi‑Browser” Shell (Zero API Keys Required)

| Capability | Detail |
|------------|--------|
| 🗂️ **Global AI Directory** | Google Gemini, OpenAI ChatGPT, Anthropic Claude, Microsoft Copilot, DeepSeek (V3 & R1), Mistral Le Chat, Meta AI, Perplexity AI, HuggingChat, DuckDuckGo AI, Poe, Qwen Chat, Phind |
| 📑 **Drop & Close Multi‑Tab Workspace** | Dynamic horizontal tab strip with provider favicons, drag‑and‑drop reordering, swipe‑to‑dismiss |
| 🔄 **WebView Instance Pooling** | Inactive tabs use `View.GONE` to preserve auth states and drafts without reloading |
| ⌨️ **Unified Floating Prompt Bar** | Write once; JavaScript DOM injection submits to the active AI provider’s chatbox |
| 🔐 **Seamless Authentication** | Desktop/Mobile Chrome User‑Agent masking, native cookie synchronization via `CookieManager` |

---

### 🛠️ Mode 2 — Developer API & Custom OAuth 2.0 PKCE Studio

| Capability | Detail |
|------------|--------|
| ⚡ **Direct Multi‑Provider Streaming** | Low‑latency HTTPS SSE for Gemini, OpenAI Chat Completions, and Anthropic Messages |
| 🔁 **Dynamic Model Fallback** | Built‑in endpoint resolution prevents HTTP 404 drops with automated routing to stable models |
| 🔑 **Custom OAuth 2.0 / PKCE Flow** | Configurable authorization code flow with PKCE S256 verification |
| 🧠 **Prompt Canvas** | Multiline system/user prompts, `{{variable_name}}` substitution, saved templates, local run history |
| 📊 **Live Estimates** | Approximate token counts and user‑defined cost estimates |

---

## ✨ Feature Matrix

### 🔐 Hardware‑Grade Security Vault

| Layer | Implementation |
|-------|----------------|
| 🛡️ **Android Keystore AES‑GCM** | Non‑exportable hardware‑backed keys with per‑entry AAD |
| 🚫 **Zero Remote Proxies** | Direct end‑to‑end TLS/HTTPS to official provider servers |
| 🔒 **Masked API Key Entry** | Credentials never displayed in plaintext |
| 🔄 **Keystore Status Check** | Reports hardware vs. software backing |

---

### 🧰 Offline Developer Toolbox (20+ Utilities)

All utilities execute **100% locally**—no network requests.

| Category | Tools |
|----------|-------|
| 📄 **Data & Schema** | JSON format/minify, XML formatting (external entities disabled) |
| 🔐 **Security & Hashes** | SHA‑256 / SHA‑512, Base64, URI component transforms |
| 🔍 **Parsing & Evaluation** | RE2/J regex tester, JWT inspector (unverified), JSON Pointer, bounded text diff |
| 🧮 **Utilities** | UUID generation, UTC timestamp converter, HEX contrast analyzer, Unicode inspector |

---

### 🎨 Resilient, Polished UI & Monetization

| Feature | Detail |
|---------|--------|
| 🖌️ **Edge‑to‑Edge Material 3** | Dynamic colors, system dark mode, glassmorphic accents, responsive layout |
| ☕ **Zero‑Shift Sponsor Fallback** | Offline or ad‑failure → stable‑height “Coffee Sponsor” card (GitHub Sponsors, Ko‑fi, Buy Me a Coffee) |
| 📢 **AdMob Next‑Gen SDK & UMP** | Strict consent checks; debug uses Google test IDs, release uses live IDs |
| 🔄 **Google Play In‑App Updates** | Flexible background downloads and immediate rollouts |

---

### 🌍 Global Localization (i18n)

| Language | Resource |
|----------|----------|
| 🇬🇧 English | `values/strings.xml` |
| 🇪🇸 Spanish | `values-es/strings.xml` |
| 🇩🇪 German | `values-de/strings.xml` |
| 🇮🇳 Hindi | `values-hi/strings.xml` |
| 🇸🇦 Arabic (RTL) | `values-ar/strings.xml` |

---

## 🏗️ Architecture

Built with **Clean Architecture + MVVM + Unidirectional Data Flow (UDF)**.

```text
app/src/main/java/com/vibesync/devstudio/
├── core/
│   ├── ads/          # AdMob Next-Gen controller & UMP consent managers
│   ├── crypto/       # Android Keystore AES/GCM vault & PKCE utilities
│   ├── network/      # ConnectivityManager reactive monitor
│   └── update/       # Play In-App Update & Review handlers
├── data/
│   ├── browser/      # Multi-provider directory & DOM injection scripts
│   ├── local/        # Room DB, run history, variable sets
│   ├── remote/       # SSE clients for Gemini, OpenAI, Claude
│   └── security/     # Keystore encryption & verification
├── domain/           # Clean Architecture use cases & models
└── presentation/
    ├── browser/      # Multi-tab WebView pool & gesture drawer
    ├── common/       # Coffee sponsor fallback, headers, cards
    ├── developer/    # Prompt canvas & OAuth settings
    └── toolbox/      # Offline developer tools suite
```


---

## ⚙️ Build Specifications

| Specification | Value |
|---------------|-------|
| 🧱 **minSdk** | **24** (Android 7.0 Nougat) |
| 🎯 **targetSdk / compileSdk** | **36** (Android 16) |
| ☕ **JDK** | **17 LTS** |
| 🟣 **Kotlin** | **2.x** |
| 🛠️ **Gradle** | **8.13** |
| 🤖 **AGP** | **8.6.0+** |

---

## 🛠️ Build & Test

### 🧪 Debug Build

```bash
gradle :app:assembleDebug
```

### 🚀 Unit Tests

```bash
gradle :app:testDebugUnitTest
```

### 🔍 Lint

```bash
gradle :app:lintDebug
```


---

### 🤖 CI/CD — GitHub Actions

The included workflow runs:

- ✅ Unit tests & Android lint
- ✅ Instrumentation tests on an **API 36 Google APIs emulator**
- ✅ Signed release **AAB** + **APK** builds on `main`
- ✅ Release package/version/signature verification
- ✅ Uploads artifacts (release bundle, APKs, R8 mapping)
  

---

## 🛡️ Security & Privacy

### Core Guarantees

| Guarantee | Implementation |
|-----------|----------------|
| 🚫 **No Analytics / Crash SDKs** | Zero telemetry; logs stay local unless explicitly exported |
| 🔒 **No Embedded Secrets** | API keys, OAuth secrets, and proxy dependencies are never bundled |
| 🔐 **Direct TLS/HTTPS** | Cleartext traffic and automatic redirects disabled |
| 🧊 **Hardware‑Backed Keystore** | AES/GCM vault with non‑exportable keys; software fallback reported honestly |

> [!NOTE]
> **Offline‑Capable, Not Network‑Isolated**
> Local tools and saved work remain fully available without internet. Provider requests, ads, and Play update checks require connectivity.

---

## 📜 License

**Copyright (c) 2026 Vibesyncrotechnologies Pvt Ltd. All rights reserved.**

**PROPRIETARY AND CONFIDENTIAL.** This software, its source code, assets, UI designs, and related documentation are the sole proprietary property of Vibesyncrotechnologies Pvt Ltd. Unauthorized copying, reverse engineering, redistribution, sublicensing, or public deployment without explicit written consent is prohibited.

- 📄 **License:** See [LICENSE](https://github.com/navyask911/VibeSync-AI-Dev-Studio/blob/main/LICENSE)
- 🔒 **Privacy Policy:** See [PRIVACY_POLICY.md](https://github.com/navyask911/VibeSync-AI-Dev-Studio/blob/main/PRIVACY_POLICY.md)
- 🌐 **Official Repository:** [https://github.com/navyask911/VibeSync-AI-Dev-Studio](https://github.com/navyask911/VibeSync-AI-Dev-Studio)

---

<div align="center">

**Made with ❤️ by VibeSync**

*Universal AI access. Developer‑grade tools. Absolute privacy.*

</div>
```
