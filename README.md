# VibeSync AI Dev Studio ⚡

A local-first native Android prompt engineering workspace built with Kotlin, Jetpack Compose, Material 3, Clean Architecture, MVVM, Ktor streaming, Room, and Android Keystore.

## What is implemented

- **Prompt canvas:** multiline system and user prompts, variable substitution with {{variable_name}}, saved templates, local run history, and variable-set persistence.
- **Live estimates:** character-based approximate input/output token counts; cost estimates use rates entered by the user because provider pricing and model availability change.
- **Streaming responses:** direct HTTPS SSE handling for OpenAI Chat Completions, Anthropic Messages, and Gemini streamGenerateContent.
- **Secure credential vault:** AES/GCM encryption with a non-exportable Android Keystore key, per-entry authenticated data, masked API key entry, and a Keystore status check.
- **Lifecycle-safe state:** StateFlow UI models, ViewModel-owned CoroutineScopes, cancellation support, and streaming flow backpressure.
- **OAuth foundation:** generic Authorization Code + PKCE utilities, a state-verifying deep-link callback, and a configurable token exchange. OAuth is disabled in the default build and its identity token is not used as a provider API key.
- **Material 3 UI:** system dark mode, Android 12+ dynamic colors, edge-to-edge layout, Markdown-style headings/lists/code fences, copy, and share/export.
- **Build validation:** GitHub Actions runs unit tests and lint, runs instrumentation tests on an Android emulator, and on main builds and uploads signed release AAB/APK artifacts plus the debug APK.

## Architecture

- domain/model: provider, prompt, variable, model configuration, history, and error models.
- domain/repository: storage/execution/credential contracts.
- domain/usecase: prompt validation/execution, variable substitution, approximate token/cost estimation, and credential management.
- data/local: Room templates, run history, variable sets, DAO, and database.
- data/security: Android Keystore AES/GCM vault and PKCE cryptographic utilities.
- data/remote: Ktor OkHttp client, provider request encoding, SSE parsing, and error mapping.
- data/repository: repository implementations and Room/domain mappings.
- presentation: Compose screens, lifecycle-aware ViewModels, OAuth callback activity, and Material 3 theme.
- VibeSyncApplication: lightweight application-scoped dependency container.

## Build requirements

- Android Studio with JDK 17
- Android SDK platform 36 and build tools 36.0.0
- Gradle 8.13

Open the repository in Android Studio after installing the listed SDK and Gradle version, or use Gradle from a terminal:

- Compile debug: gradle :app:assembleDebug
- Unit tests: gradle :app:testDebugUnitTest
- Lint: gradle :app:lintDebug

GitHub Actions executes these checks on pushes and pull requests. The generated debug APK is available as a workflow artifact when the job passes.

## Provider API keys

In Settings, select a provider and save its API key. The value is encrypted with AES/GCM before being persisted. Provider requests go directly from the device to the provider over HTTPS; there are no built-in proxies, telemetry, or embedded credentials.

A ChatGPT subscription is not an OpenAI API key. For OpenAI API requests, use an API key issued for the API platform with the required API billing configuration. A ChatGPT OAuth identity token is deliberately kept separate from API keys.

## Optional OAuth configuration

The default build intentionally disables OAuth until you have an OAuth client registered and authorized for this Android app. Set these Gradle properties in a local Gradle properties file or the build environment:

- VIBESYNC_OAUTH_CLIENT_ID
- VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT
- VIBESYNC_OAUTH_TOKEN_ENDPOINT

Both endpoints must be HTTPS. The app uses the redirect URI vibesync://auth/callback; register that exact URI with the identity provider if supported. The implementation performs PKCE S256 and validates a random state value before exchanging the code. Do not add provider client secrets to the APK. Configuring these properties does not make an unsupported provider OAuth flow valid, and the resulting OAuth token is not used to authenticate provider model requests.

## Security notes

- Cleartext network traffic and automatic redirects are disabled.
- No API keys, OAuth client secrets, or external proxy dependencies are embedded.
- Android Keystore may be hardware-backed or software-backed depending on device capabilities; the app reports what the platform says.
- If Android Keystore state is lost or invalidated, previously encrypted credentials may need to be re-entered.
- Token counts are estimates, not exact model tokenizer counts or provider-reported usage.

## Verification status

A CI workflow is included for Gradle compilation, Android lint, and unit tests. Physical-device testing of API provider accounts, OAuth registration, interruption/retry behavior, and hardware-backed Keystore behavior remains necessary before a production Play Store release.

## License

Copyright (c) 2026 Vibesyncrotechnologies Pvt Ltd. All rights reserved.

PROPRIETARY AND CONFIDENTIAL. This software, its source code, assets, UI designs, and related documentation are the sole proprietary property of Vibesyncrotechnologies Pvt Ltd. Unauthorized copying, reverse engineering, redistribution, sublicensing, or public deployment without explicit written consent is prohibited.

## Developer diagnostics, connectivity, and release features

- **Developer Diagnostics** is available from the prompt screen and Settings. It stores a bounded local record history for app-owned events, provider HTTP status/duration/first-stream-chunk/failure metadata, vault results, connectivity transitions, and uncaught crash summaries. Prompts, generated responses, API keys, and authorization headers are not intentionally recorded.
- Diagnostic exports are written under app-private storage and shared with temporary FileProvider read permission. The viewer can filter severity, component, keyword, and time range. Android restricts full device-wide Logcat access for ordinary apps; use Android Studio/ADB or an Android bug report when system logs are required.
- Connectivity status distinguishes offline, connecting/unvalidated, and validated internet access. Local prompt editing, saved templates, and local history remain available offline.
- Google Play In-App Updates and In-App Review are best-effort integrations. They are not available on every sideloaded installation and do not block core app use.
- Google Mobile Ads banner and interstitial components use Google's preferred Mobile Ads Next-Gen SDK 1.5.0. UMP consent is checked before ads are requested. Debug builds use Google's sample app/banner/interstitial identifiers; release builds use the configured publisher IDs. Interstitials are considered only after at least three successful prompt runs and no more than once per ten minutes, and are skipped if no ad is already loaded.
- Google Play reviews are requested only after repeated successful use. Google Play chooses whether to show its review dialog.
- Version defaults: VERSION_CODE=20261023, VERSION_NAME=1.0.4. If the Google Play Console has a later used version code, set a greater -PVERSION_CODE=... before release.

### Ad privacy and configuration

Google's User Messaging Platform (UMP) handles consent-information refresh and any configured required consent form. If UMP reports a privacy-options entry point is required, Settings > Developer tools / Ad privacy choices opens that form. Publisher-specific privacy messages and policy settings must still be configured in the AdMob account; inclusion of the SDK alone does not prove that the publisher's account consent configuration is complete.

Ad IDs are split by build variant:
- Release App ID: ca-app-pub-4783826505860771~3225002433
- Release banner: ca-app-pub-4783826505860771/3927535003
- Release interstitial: ca-app-pub-4783826505860771/4941233773

The debug build uses the official Google sample IDs. Do not click live release ads during testing. CI validates code/configuration and APK/AAB signatures, but real ad serving and consent presentation still require an appropriate test device, network access, and a correctly configured AdMob account.

### CI artifacts and tests

CI runs unit tests and lint, boots an API 36 Google APIs emulator for Android instrumentation tests, builds debug and signed release outputs on main, verifies the release package/version/signature, and uploads the release bundle, APKs, and (when present) R8 mapping file.


### Offline Developer Toolbox (initial phase)

The searchable toolbox is separate from the prompt editor and stores favorites/recent tools locally. The initial offline tools include JSON format/minify, XML formatting with external entities disabled, bounded text diff, Base64, URI component encoding, JSON Pointer, linear-time RE2/J regex testing with capture groups, JWT header/payload decoding (explicitly unverified), SHA-256/SHA-512 digests, capped UUID generation, UTC timestamp conversion, HEX contrast ratios, Unicode inspection, text transformations, and compact HTTP status/MIME references. The URI inspector redacts common secret query values and never launches a link. Tools enforce input/output bounds and have unit-test coverage. Tools not listed here remain future implementation work; this is not yet the entire catalogue proposed in the master prompt.


## Offline operation, privacy, localization, and support

- JSON/XML formatting, diffing, Base64/URI transforms, JSON Pointer, regex testing, JWT decoding, hashes, timestamps, contrast, Unicode inspection, and other Developer Toolbox utilities execute locally on this device and do not issue network requests.
- The app adds no analytics, crash-reporting, or remote diagnostic SDK and does not upload app-collected diagnostics. Logs remain local unless explicitly exported/shared. Provider API requests go directly to the selected provider only when a user runs a prompt; prompts and responses are excluded from the app diagnostics.
- Optional AdMob/UMP and Google Play update/review integrations contact Google services when active. The app is therefore offline-capable rather than a completely network-isolated sandbox: local tools and saved work remain available without internet, while provider requests, ads, and Play update checks require connectivity. Sponsor links open only after a user taps them.
- English, Spanish, German, Hindi, and Arabic resource files are provided. RTL support is enabled in the manifest.
- Ad units are exposed through variant-specific BuildConfig fields. Debug uses Google's official test IDs. Release reads `ADMOB_APP_ID`, `ADMOB_BANNER_UNIT_ID`, and `ADMOB_INTERSTITIAL_UNIT_ID` Gradle properties (the current publisher IDs are defaults); override with `-P...` or in `~/.gradle/gradle.properties`.
- When offline, ad consent is unavailable, SDK initialization is pending, or an ad load fails, the ad area uses a stable-height, animated Coffee Sponsor fallback. Ads are not required for any local workflow.

# VibeSync AI Dev Studio ⚡

<p align="center">
  <em>Universal AI Multi-Browser Shell, Developer Studio & Privacy-First Prompt Workspace</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"/>
  <img src="https://img.shields.io/badge/Min_SDK-26_(Android_8.0)-blue.svg" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Target_SDK-36_(Android_15)-blue.svg" alt="Target SDK"/>
  <img src="https://img.shields.io/badge/Language-Kotlin_100%25-purple.svg" alt="Language"/>
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-orange.svg" alt="UI Framework"/>
  <img src="https://img.shields.io/badge/License-Proprietary_%2F_Source--Available-red.svg" alt="License"/>
</p>

---

## Overview

**VibeSync AI Dev Studio** is an advanced, dual-mode native Android workspace developed by **Navyask911** and **Vibesyncro Technologies Pvt Ltd**. Engineered to eliminate the barrier between paid developer APIs and consumer AI access, it unifies worldwide conversational AI engines into a high-performance, single-app experience while protecting user privacy.

Built on an uncompromising **Strict Privacy-First, Zero-Telemetry Mandate**, VibeSync guarantees that all user prompts, web session cookies, API tokens, and local utilities remain entirely confined to the device.

---

## Dual-Mode Architecture

### 1. Universal "AI Multi-Browser" Shell (Zero API Keys Required)
- **Global AI Directory:** Direct, instant access to leading free conversational web platforms:
  - Google Gemini, OpenAI ChatGPT, Anthropic Claude, Microsoft Copilot, DeepSeek (V3 & R1), Mistral Le Chat, Meta AI, Perplexity AI, HuggingChat, DuckDuckGo AI (Private/No-Login), Poe, Qwen Chat, and Phind.
- **Drop & Close Multi-Tab Workspace:**
  - Dynamic horizontal tab strip with provider favicons and active indicator glow.
  - Tab Drawer supporting **drag-and-drop reordering** and **swipe-to-dismiss** close mechanics.
  - Inactive `WebView` instance pooling (`View.GONE`) to preserve authentication states, active drafts, and streaming text without reloading.
- **Unified Floating Prompt Bar:**
  - Write queries or code directives once in the persistent studio prompt bar.
  - Automatic JavaScript DOM injection evaluates and triggers submission directly into the active AI provider's chatbox.
- **Seamless Authentication:**
  - Desktop/Mobile Chrome User-Agent masking eliminates Google, Microsoft, and Apple OAuth "disallowed user-agent" blocks.
  - Native cookie synchronization via `CookieManager` maintains persistent sessions across app restarts.

### 2. Developer API & Custom OAuth 2.0 PKCE Studio
- **Direct Multi-Provider Streaming:** Low-latency HTTPS Server-Sent Events (SSE) streaming for Google Gemini (`generateContent` & `streamGenerateContent`), OpenAI Chat Completions, and Anthropic Messages.
- **Dynamic Model Fallback:** Built-in endpoint resolution preventing HTTP 404 connection drops with automated routing to stable models.
- **Custom OAuth 2.0 / PKCE Flow:** Configurable authorization code flow with PKCE S256 verification for developers bringing their own registered client credentials.

---

## Key Platform Features

### Hardware-Grade Security Vault
- **Android Keystore AES-GCM:** User-supplied API keys and sensitive tokens are encrypted using non-exportable hardware-backed keys with per-entry authenticated data (AAD).
- **Zero Remote Proxies:** All API calls and web sessions establish direct, end-to-end TLS/HTTPS connections with official provider servers.

### Offline Developer Toolbox
Integrated, zero-network developer utilities that execute 100% locally on-device:
- **Data & Schema:** JSON formatting, schema minification, and XML formatting with external entities disabled.
- **Security & Hashes:** SHA-256 and SHA-512 cryptographic digests, Base64 encode/decode, and URI component transforms.
- **Parsing & Evaluation:** Linear-time RE2/J regex tester with capture group extraction, unverified JWT header/payload inspector, JSON Pointer query tool, and bounded text diff comparisons.
- **Utilities:** UUID generation, UTC timestamp converters, HEX contrast analyzers, and Unicode character inspectors.

### Resilient, Polished UI & Monetization
- **Edge-to-Edge Material 3:** Adaptive dynamic coloring, system dark mode, glassmorphic accents, and responsive layout constraints that prevent header title truncation.
- **Zero-Shift Sponsor Fallback:** Reactive `ConnectivityManager` detection; if the device goes offline or ad loading fails, ad slots gracefully fold into a stable-height "Coffee Sponsor" card (GitHub Sponsors, Ko-fi, Buy Me a Coffee).
- **AdMob Next-Gen SDK & UMP:** Strict European and global User Messaging Platform consent checks with variant-specific build configurations (Google sample IDs on Debug, live IDs on Release).
- **Google Play In-App Updates:** Integrated `app-update-ktx` supporting both flexible background downloads and immediate rollouts.

---

## Global Localization (i18n)

VibeSync AI Dev Studio extracts 100% of user-facing strings into resource files and supports:
- **English** (`values/strings.xml`)
- **Spanish** (`values-es/strings.xml`)
- **German** (`values-de/strings.xml`)
- **Hindi** (`values-hi/strings.xml`)
- **Arabic** (`values-ar/strings.xml`) with comprehensive Right-to-Left (RTL) layout rendering.

---

## Repository Structure

```text
VibeSync-AI-Dev-Studio/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vibesync/devstudio/
│   │   │   │   ├── core/
│   │   │   │   │   ├── ads/          # AdMob Next-Gen controller & consent managers
│   │   │   │   │   ├── crypto/       # Android Keystore AES/GCM vault & PKCE utilities
│   │   │   │   │   ├── network/      # ConnectivityManager reactive monitor
│   │   │   │   │   └── update/       # Play In-App Update & Review handlers
│   │   │   │   ├── data/
│   │   │   │   │   ├── browser/      # Multi-provider directory & DOM injection scripts
│   │   │   │   │   ├── local/        # Room Database, history, and variable sets
│   │   │   │   │   ├── remote/       # SSE network clients for Gemini, OpenAI, Claude
│   │   │   │   │   └── security/     # Keystore encryption & verification
│   │   │   │   ├── domain/           # Clean Architecture Use Cases & Models
│   │   │   │   └── presentation/
│   │   │   │       ├── browser/      # Multi-tab WebView pool & gesture drawer
│   │   │   │       ├── common/       # Coffee sponsor fallback, headers, cards
│   │   │   │       ├── developer/    # Prompt canvas & OAuth settings
│   │   │   │       └── toolbox/      # Offline developer tools suite
│   │   │   ├── res/                  # Multi-lingual strings & Material 3 drawables
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/                # CI workflow: Unit tests, Lint, Emulator, Release Builds
└── README.md


## AI Web Studio

AI Web Studio provides multi-tab browsing for the provider websites in the app directory. WebView tabs remain alive when inactive and their views are destroyed when their tabs are closed. The prompt helper uses best-effort DOM selectors on known provider hosts; providers may change their pages, so the helper may not work on every page. It never exposes a JavaScript bridge to provider pages.

The app retains Android WebView's platform user-agent instead of spoofing Chrome to bypass sign-in restrictions. Use **Open in system browser** when a provider requires a full browser. System-browser sessions are not copied to the embedded WebView.

Website sign-in is separate from provider API credentials. API mode continues to use the encrypted Android Keystore vault. Google sign-in in OAuth settings is identity-only; it is not a replacement for a Gemini API key. OpenAI and Anthropic API presets explain their API-key requirement, while custom OAuth settings are for a developer-owned public client and endpoint.

Model discovery uses each provider's models endpoint when a saved API key is available and falls back to curated suggestions offline. Gemini streaming keeps the documented `streamGenerateContent` endpoint; a 404 on the selected model triggers one retry with `gemini-3.8-flash` and an app-local diagnostic event.
